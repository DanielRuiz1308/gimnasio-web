package com.gimnasio.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class ConexionBD {

    private static final String URL = leer("DB_URL", "jdbc:mysql://localhost:3306/gimnasio_db");
    private static final String USUARIO = leer("DB_USER", "root");
    private static final String CONTRASENA = leer("DB_PASSWORD", "tu_clave");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CONTRASENA);
    }

    private static String leer(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return (valor == null || valor.isBlank()) ? porDefecto : valor;
    }
}
