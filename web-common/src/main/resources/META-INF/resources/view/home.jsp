<!doctype html>
<html lang="${projectDefaultLocale}">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>DBsync</title>
    <base href="/">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
    <meta http-equiv="Pragma" content="no-cache">
    <meta http-equiv="Expires" content="0">
    <link rel="stylesheet" href="styles.${projectBuildNumber}.css">
</head>
<body>
<%@ include file="header.jsp" %>
<app-root></app-root>
<script src="runtime.${projectBuildNumber}.js" defer></script>
<script src="polyfills.${projectBuildNumber}.js" defer></script>
<script src="scripts.${projectBuildNumber}.js" defer></script>
<script src="vendor.${projectBuildNumber}.js" defer></script>
<script src="main.${projectBuildNumber}.js" defer></script>
</body>
</html>
