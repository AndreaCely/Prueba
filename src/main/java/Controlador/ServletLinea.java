package Controlador;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.JOptionPane;

import Modelo.LineasDAO;
import Modelo.LineasDTO;

/**
 * Servlet implementation class ServletLinea
 */
@WebServlet("/ServletLinea")
public class ServletLinea extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ServletLinea() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		int codigo;
		String nombre;
		boolean x;
		
		LineasDTO linDTO;
		LineasDAO linDAO;
		
		if(request.getParameter("buttonInsert") != null) {
			codigo = Integer.parseInt(request.getParameter("codigo"));
			nombre = request.getParameter("nombre");
			linDTO = new LineasDTO(codigo, nombre);
			linDAO = new LineasDAO();
			x= linDAO.insertarlinea(linDTO);
			
			if (x == true) {
				JOptionPane.showMessageDialog(null, "Registro Correctamente");
				response.sendRedirect("index.jsp");				
			}
			else {
				JOptionPane.showMessageDialog(null, "Error");
				response.sendRedirect("index.jsp");	
			}
			
		}
		/*
		if (u.equals("pepe")&&c.equals("123")) {
			JOptionPane.showMessageDialog(null, "datos Correctos");
			response.sendRedirect("vistados.jsp?dat="+u);
		}
		else {
			JOptionPane.showMessageDialog(null, "datos Incorrectos");
			response.sendRedirect("index.jsp");
		}*/
	}

}
