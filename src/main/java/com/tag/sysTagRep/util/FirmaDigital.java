package com.tag.sysTagRep.util;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
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
 * Firma digital de XMLs usando certificado .p12 (XMLDSig, XAdES-BES).
 * Agrega el bloque &lt;ds:Signature&gt; como último hijo del elemento raíz
 * con id="comprobante", tal como exige el SRI.
 */
public class FirmaDigital {

    private static final XMLSignatureFactory FACTORY = obtenerFactory();

    private PrivateKey privateKey;
    private X509Certificate certificate;

    private static XMLSignatureFactory obtenerFactory() {
        try {
            Provider sun = (Provider) Class.forName("org.jcp.xml.dsig.internal.dom.XMLDSigRI").getDeclaredConstructor().newInstance();
            return XMLSignatureFactory.getInstance("DOM", sun);
        } catch (Exception e) {
            return XMLSignatureFactory.getInstance("DOM");
        }
    }

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
     * Firma un XML y retorna el XML firmado con el bloque ds:Signature.
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

        String algoritmoFirma = "RSA".equalsIgnoreCase(privateKey.getAlgorithm())
                ? SignatureMethod.RSA_SHA256
                : SignatureMethod.ECDSA_SHA256;

        CanonicalizationMethod canMeth = FACTORY.newCanonicalizationMethod(
                CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null);
        DigestMethod digMeth = FACTORY.newDigestMethod(DigestMethod.SHA256, null);
        Transform enveloped = FACTORY.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);
        Reference ref = FACTORY.newReference("#" + id, digMeth,
                Collections.singletonList(enveloped), null, null);
        SignedInfo signedInfo = FACTORY.newSignedInfo(canMeth, FACTORY.newSignatureMethod(algoritmoFirma, null),
                Collections.singletonList(ref));

        KeyInfoFactory kif = FACTORY.getKeyInfoFactory();
        X509Data x509Data = kif.newX509Data(Collections.singletonList(certificate));
        KeyInfo keyInfo = kif.newKeyInfo(Collections.singletonList(x509Data));

        XMLSignature firma = FACTORY.newXMLSignature(signedInfo, keyInfo);
        DOMSignContext context = new DOMSignContext(privateKey, raiz);
        firma.sign(context);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    public boolean isCertificadoCargado() {
        return privateKey != null && certificate != null;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }
}
