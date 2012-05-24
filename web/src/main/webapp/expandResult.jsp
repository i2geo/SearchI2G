<%-- Result showing query expansion
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head><title>I2G Search Query</title></head>
  <body>

    <h1>I2G Search Query</h1>

    <form method="GET" action="expand"><%-- TODO: path to query-tester --%>
        <input type="text" size="50" name="q"/>

    </form>


    <p>You searched for:
        <b><%=request.getAttribute("userQ")%></b>
    </p>
<hr/>
  <p>
      <%=request.getAttribute("luceneQ").toString()%>
  </p>


  </body>
</html>