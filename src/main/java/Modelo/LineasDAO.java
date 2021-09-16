package Modelo;
import Controlador.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import Controlador.Conexion; 
public class LineasDAO {
 Conexion con=new Conexion();
 Connection cnn=con.conexionbd();
 PreparedStatement ps;


public boolean insertarlinea(LineasDTO lin) {
    int r;
    boolean dat=false;
    try {
		ps=cnn.prepareStatement("INSERT INTO lineas values(?,?)");
		ps.setInt(1, lin.getCodigoline());
		ps.setString(2, lin.getNombrelinea());
		r=ps.executeUpdate();
		if(r>0) {
			dat=true;
		}
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    return dat;
}
}