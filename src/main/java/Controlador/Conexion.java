package Controlador;

import java.sql.*;

import javax.swing.JOptionPane;

public class Conexion {
Connection cnn;


public Connection conexionbd() {
	
    try {
		Class.forName("com.mysql.cj.jdbc.Driver");
		cnn=DriverManager.getConnection("jdbc:mysql://localhost/bancos","root","root");
	    //JOptionPane.showMessageDialog(null, "Conexion okdfsd");
    } catch (ClassNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
return cnn;
}
}