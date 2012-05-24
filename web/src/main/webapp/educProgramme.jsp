<%@ page import="net.i2geo.onto.GeoSkillsAccess" %>
<%--
    Parameters: uri: the URI of the individual
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // setup params
    String progURI = request.getParameter("uri");
    if(progURI==null) {
        progURI = request.getPathInfo();
        if(progURI.startsWith("educProgramme.jsp"))
            progURI = progURI.substring("educProgramme.jsp".length());
        if(progURI.startsWith("/"))
            progURI = progURI.substring(1);
    }

    // set-up ontology connection
    GeoSkillsAccess access = GeoSkillsAccess.getInstance();
    

%>
<html>
  <head><title>Educational Programme - </title></head>
  <body>Place your content here</body>
</html>