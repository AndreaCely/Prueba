
<%@page import="Controlador.Conexion"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Insert title here</title>
</head>
<body>
Hola
<%
Conexion con=new Conexion(); 
con.conexionbd();
%>
<form action="ServletLinea" method = "post">
	<input type="text"name="codigo">
	<input type="text"name="nombre">
	<input type="submit" name="buttonInsert">
</form>

</body>
</html>