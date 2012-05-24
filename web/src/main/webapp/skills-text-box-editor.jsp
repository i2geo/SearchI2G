<%@ page import="net.i2geo.api.SKBi18n" %>
<%@ page import="net.i2geo.search.SKBPropsI18n" %>
<%@ page import="net.i2geo.search.I2GSearchWebappCenter" %>
<%--
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<%
  String types = request.getParameter("types");
  if(types==null) types="";
  String value = request.getParameter("value");
  if(value==null) value="";
  String setValueFunction = request.getParameter("setValueFunction");

  String paramLanguage = request.getParameter("language");
  String languages;
  if(paramLanguage ==null) languages = request.getHeader("Accept-Language");
  else languages = paramLanguage + ";" + request.getHeader("Accept-Language");
  String firstLanguage = languages;
  if(firstLanguage.contains(";")) firstLanguage = firstLanguage.substring(0,firstLanguage.indexOf(";"));
  if(firstLanguage.contains("-")) firstLanguage = firstLanguage.substring(0,firstLanguage.indexOf("-"));


    String title = request.getParameter("title");
    if(title==null) title = I2GSearchWebappCenter.getInstance().tryToGetOrMakeServiceForLangs(languages).getI18n().editorTitle();


  String debug = request.getParameter("debug");
  if(debug==null || debug.length()==0) debug = "false";
%>
  <head>
      <title><%=title%></title>
      <script type="text/javascript">
          window.skbPleaseReplaceMeActive =  "SkillsTextBox|idsStorage| <%=types%> |<%=debug%>";
          <% if(setValueFunction!=null) {%>window.setValueFunction = <%=setValueFunction%>;<%}%>
          window.skbConfigBasePath = "/SearchI2G/net.i2geo.skillstextbox.SkillsTextBox/";
          window.browserLanguages = "<%=languages%>";
      </script>
      <script language='javascript' src='/static/JStrans/JStrans-<%=firstLanguage%>.js'></script>
      <script language='javascript' src='/SearchI2G/net.i2geo.skillstextbox.SkillsTextBox/net.i2geo.skillstextbox.SkillsTextBox.nocache.js'></script>
      <link rel="stylesheet" type="text/css"  href="/SearchI2G/net.i2geo.skillstextbox.SkillsTextBox/SkillsTextBox.css" />
      <style type="text/css">body { font-family:arial;}</style>
      <meta name="gwt:module" content="net.i2geo.skillstextbox.SkillsTextBox">
  </head>
  <body>


  <h3><%=title%></h3>
  <form action="" onsubmit="return false;">
      <div id="SkillsTextBox"></div>
      <!-- <p><a href="javascript:window.skbEdit('SkillsTextBox','idsStorage', '<%=types%>','false'); this.setVisible(false);">edit</a></p> -->
      <!-- <input id="idsStorage" name="idsStorage_" value="<%=value%>"/> -->
      <!-- <p>makes strings:</p> -->
      <input id="idsStorage" name="idsStorage_" value="<%=value%>" size="100"
      <%if("true".equals(request.getParameter("debug"))){%>type="text"<%}else{%>type="hidden"<%}%>/>

  </form>

  <!--<p><a onclick="return openEditor();">open another window</a></p>-->

  <script type="text/javascript">
      var objectHere = document.getElementById("idsStorage");
      function setIt(value) {
          alert("Value set " + value);
      }
      function openEditor() {
          var w = window.open('skills-text-box-editor.jsp','skills-text-box-editor','width=520,height=250,scrollbars=yes,status=no,toolbar=no,menubar=no,location=no,resizable=yes');
          w.setMyValue = window.setIt; 
          return false;
      }
  </script>
  </body>
</html>