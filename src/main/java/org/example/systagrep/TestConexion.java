package org.example.systagrep;
import com.tag.sysTagRep.dao.VendedorDAO;
import com.tag.sysTagRep.model.Vendedor;

public class TestConexion {
    public static void main(String[] args) {

        Vendedor vendedor = new Vendedor(
                "Carlos Lopez",
                "0912345678",
                "carlos@mail.com",
                true
        );

        VendedorDAO dao = new VendedorDAO();
        dao.guardar(vendedor);
    }
}
