<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Procesador de Imágenes</title>
    <link rel="stylesheet" href="styles/styles.css"> <!-- Ruta relativa al archivo CSS -->
</head>
<body>
<div class="container">
    <h1>Procesador de Imágenes</h1>
    <form action="ImageProcessorServlet" method="post" enctype="multipart/form-data">
        <input type="file" name="imagen" accept="image/*" required>
        <input type="submit" value="Procesar">
    </form>
</div>
</body>
</html>
