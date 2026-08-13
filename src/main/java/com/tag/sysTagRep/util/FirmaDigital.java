package com.tag.sysTagRep.util;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Firma digital de XMLs usando certificado .p12 bajo el estándar XAdES-BES
 * (esquema ETSI 1.3.2), tal como exige el SRI (ficha técnica, sección 6 y anexo 14).
 * <p>
 * Genera &lt;ds:Signature&gt; como último hijo de la raíz id="comprobante", con
 * SignedInfo (referencias a SignedProperties, KeyInfo y comprobante), SignatureValue,
 * KeyInfo con la cadena de certificados y un ds:Object con
 * etsi:QualifyingProperties/etsi:SignedProperties (SigningTime, SigningCertificate,
 * DataObjectFormat).
 */
public class FirmaDigital {

    private static final String NAMESPACE_DS = "http://www.w3.org/2000/09/xmldsig#";
    private static final String NAMESPACE_ETSI = "http://uri.etsi.org/01903/v1.3.2#";
    private static final String NAMESPACE_XMLNS = "http://www.w3.org/2000/xmlns/";
    private static final String TIPO_SIGNED_PROPERTIES = "http://uri.etsi.org/01903#SignedProperties";
    private static final ZoneOffset ZONA_ECUADOR = ZoneOffset.ofHours(-5);

    static {
        try {
            Object proveedor = Class.forName("org.jcp.xml.dsig.internal.dom.XMLDSigRI")
                    .getDeclaredConstructor().newInstance();
            if (proveedor instanceof Provider) {
                Security.addProvider((Provider) proveedor);
            }
        } catch (Exception e) {
            // el proveedor ya suele estar registrado en el JDK
        }
    }

    private PrivateKey privateKey;
    private X509Certificate certificate;
    private X509Certificate[] certificateChain;

    /**
     * Carga el certificado digital .p12
     * @param rutaP12 ruta al archivo .p12
     * @param password contraseña del certificado
     */
    public boolean cargarCertificado(String rutaP12, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(rutaP12), password.toCharArray());

            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias)) {
                    privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
                    certificate = (X509Certificate) keyStore.getCertificate(alias);
                    java.security.cert.Certificate[] cadena = keyStore.getCertificateChain(alias);
                    if (cadena != null && cadena.length > 0) {
                        certificateChain = new X509Certificate[cadena.length];
                        for (int i = 0; i < cadena.length; i++) {
                            certificateChain[i] = (X509Certificate) cadena[i];
                        }
                    } else {
                        certificateChain = new X509Certificate[] { certificate };
                    }
                    return privateKey != null && certificate != null;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Firma un XML y retorna el XML firmado con el bloque ds:Signature (XAdES-BES).
     * @param xml XML a firmar (debe tener raíz con id="comprobante")
     */
    public String firmarXml(String xml) throws Exception {
        if (privateKey == null || certificate == null) {
            throw new IllegalStateException("Certificado no cargado. Llame cargarCertificado(...) primero.");
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        Element raiz = doc.getDocumentElement();
        String id = raiz.getAttribute("id");
        if (id == null || id.isEmpty()) {
            id = "comprobante";
            raiz.setAttribute("id", id);
        }
        raiz.setIdAttribute("id", true);

        long rnd = ThreadLocalRandom.current().nextLong() & Long.MAX_VALUE;
        String idFirma = "Signature" + rnd;
        String idSignedProperties = idFirma + "-SignedProperties" + rnd;
        String idKeyInfo = "Certificate" + rnd;
        String idRefComprobante = "Reference-ID-" + rnd;

        XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");

        CanonicalizationMethod c14n = factory.newCanonicalizationMethod(
                CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null);
        SignatureMethod sigMethod = factory.newSignatureMethod(SignatureMethod.RSA_SHA256, null);
        DigestMethod digestMethod = factory.newDigestMethod(DigestMethod.SHA256, null);
        Transform enveloped = factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);

        Reference refProps = factory.newReference("#" + idSignedProperties, digestMethod, null,
                TIPO_SIGNED_PROPERTIES, "SignedPropertiesID" + rnd);
        Reference refCert = factory.newReference("#" + idKeyInfo, digestMethod, null, null,
                "CertificateRefID" + rnd);
        Reference refComprobante = factory.newReference("#" + id, digestMethod,
                Collections.singletonList(enveloped), null, idRefComprobante);

        SignedInfo signedInfo = factory.newSignedInfo(c14n, sigMethod,
                Arrays.asList(refProps, refCert, refComprobante));

        KeyInfoFactory kif = factory.getKeyInfoFactory();
        List<Object> x509 = new ArrayList<>();
        X509Certificate[] cadena = (certificateChain != null && certificateChain.length > 0)
                ? certificateChain
                : new X509Certificate[] { certificate };
        for (X509Certificate c : cadena) {
            if (c != null) x509.add(c);
        }
        X509Data x509Data = kif.newX509Data(x509);
        KeyInfo keyInfo = kif.newKeyInfo(Collections.singletonList(x509Data), idKeyInfo);

        Element qualProps = doc.createElementNS(NAMESPACE_ETSI, "etsi:QualifyingProperties");
        qualProps.setAttributeNS(null, "Target", "#" + idFirma);

        Element signedProps = doc.createElementNS(NAMESPACE_ETSI, "etsi:SignedProperties");
        signedProps.setAttributeNS(null, "Id", idSignedProperties);
        signedProps.setAttributeNS(NAMESPACE_XMLNS, "xmlns", NAMESPACE_DS);
        signedProps.setAttributeNS(NAMESPACE_XMLNS, "xmlns:ds", NAMESPACE_DS);
        signedProps.setAttributeNS(NAMESPACE_XMLNS, "xmlns:etsi", NAMESPACE_ETSI);

        Element signedSigProps = doc.createElementNS(NAMESPACE_ETSI, "etsi:SignedSignatureProperties");

        Element signingTime = doc.createElementNS(NAMESPACE_ETSI, "etsi:SigningTime");
        signingTime.appendChild(doc.createTextNode(fechaFirma()));
        signedSigProps.appendChild(signingTime);

        Element signingCert = doc.createElementNS(NAMESPACE_ETSI, "etsi:SigningCertificate");
        Element certEl = doc.createElementNS(NAMESPACE_ETSI, "etsi:Cert");
        Element certDigest = doc.createElementNS(NAMESPACE_ETSI, "etsi:CertDigest");
        Element digestMethodEl = doc.createElementNS(NAMESPACE_DS, "ds:DigestMethod");
        digestMethodEl.setAttributeNS(null, "Algorithm", DigestMethod.SHA256);
        certDigest.appendChild(digestMethodEl);
        Element digestValueEl = doc.createElementNS(NAMESPACE_DS, "ds:DigestValue");
        digestValueEl.appendChild(doc.createTextNode(Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()))));
        certDigest.appendChild(digestValueEl);
        certEl.appendChild(certDigest);

        Element issuerSerial = doc.createElementNS(NAMESPACE_ETSI, "etsi:IssuerSerial");
        Element issuerName = doc.createElementNS(NAMESPACE_DS, "ds:X509IssuerName");
        issuerName.appendChild(doc.createTextNode(certificate.getIssuerX500Principal().getName()));
        issuerSerial.appendChild(issuerName);
        Element serial = doc.createElementNS(NAMESPACE_DS, "ds:X509SerialNumber");
        serial.appendChild(doc.createTextNode(certificate.getSerialNumber().toString()));
        issuerSerial.appendChild(serial);
        certEl.appendChild(issuerSerial);

        signingCert.appendChild(certEl);
        signedSigProps.appendChild(signingCert);
        signedProps.appendChild(signedSigProps);

        Element signedDataProps = doc.createElementNS(NAMESPACE_ETSI, "etsi:SignedDataObjectProperties");
        Element dataObjectFormat = doc.createElementNS(NAMESPACE_ETSI, "etsi:DataObjectFormat");
        dataObjectFormat.setAttributeNS(null, "ObjectReference", "#" + idRefComprobante);
        Element description = doc.createElementNS(NAMESPACE_ETSI, "etsi:Description");
        description.appendChild(doc.createTextNode("contenido comprobante"));
        dataObjectFormat.appendChild(description);
        Element mimeType = doc.createElementNS(NAMESPACE_ETSI, "etsi:MimeType");
        mimeType.appendChild(doc.createTextNode("text/xml"));
        dataObjectFormat.appendChild(mimeType);
        signedDataProps.appendChild(dataObjectFormat);
        signedProps.appendChild(signedDataProps);

        qualProps.appendChild(signedProps);

        XMLObject object = factory.newXMLObject(
                Collections.singletonList(new DOMStructure(qualProps)), idFirma + "-Object" + rnd, null, null);

        XMLSignature firma = factory.newXMLSignature(signedInfo, keyInfo,
                Collections.singletonList(object), idFirma, null);

        DOMSignContext context = new DOMSignContext(privateKey, raiz);
        context.setIdAttributeNS(signedProps, null, "Id");
        firma.sign(context);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    private static String fechaFirma() {
        return OffsetDateTime.now(ZONA_ECUADOR).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
    }

    public boolean isCertificadoCargado() {
        return privateKey != null && certificate != null;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }
}
