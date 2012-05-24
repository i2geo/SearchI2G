<%--
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<%
  // example: skbSearchField|Topic,Competency|choiceIdentified
  String title = request.getParameter("title");
  if(title==null) title = "Skills Text Box Edit Example";
  String types = request.getParameter("types");
  if(types==null) types="";
    String value = request.getParameter("value");
    if(value==null || value.length()==0) value="[Circle_r,Segment_r]";
    
%>
  <head>
      <title><%=title%></title>
      <script type="text/javascript">
          window.skbConfigBasePath = "/SearchI2G/net.i2geo.skillstextbox.SkillsTextBox/";
          window.browserLanguages = "<%=request.getHeader("Accept-Language")%>";
          window.functionNameForEditorClosing = "skillsTextBoxClosing";
      </script>
      <script language='javascript' src='/SearchI2G/net.i2geo.skillstextbox.SkillsTextBox/net.i2geo.skillstextbox.SkillsTextBox.nocache.js'></script>
      <link rel="stylesheet" type="text/css"  href="/SearchI2G/net.i2geo.skillstextbox.SkillsTextBox/SkillsTextBox.css" />
  </head>
  <body>


  <h3><%=title%></h3>
  <form action="" onsubmit="return false;">
      <div id="listOfTopics">
          <ul>
              <li>a relationship 1</li>
              <li>a relationship 2</li>
              <li>a relationship 3</li>
          </ul>
      </div>
      <input type="text" name="idsStorage" id="idsStorage_" value="<%=value%>"/><!-- <%=value%> -->
      <p align="right"><a href="javascript:window.skbDoEdit('listOfTopics|idsStorage| <%=types%> |false');">edit list</a></p>
  </form>

  <script type="text/javascript">
      window.skillsTextBoxClosing = new Function("idsList","handleSkillsTextBoxClosing(idsList);");
      function handleSkillsTextBoxClosing(idsList) {
          alert("Time to submit" + idsList);
      }

  </script>

  </body>
</html>