# Chat Multicliente

Trabajo para la asignatura de Programación de Servicios y Procesos.

## 📄 Descripción

Esta es una aplicación cliente-servidor en la que el servidor permanece a la espera de conexiones entrantes y crea un hilo para gestionar cada una de ellas, lo que le permite manejar múltiples clientes simultáneamente. Cada hilo se encarga de recibir solicitudes y enviar las respuestas correspondientes. Por otro lado, el cliente cuenta con una interfaz gráfica que inicia un hilo dedicado a escuchar las respuestas del servidor, actualizando la interfaz en tiempo real y gestionando el envío de solicitudes.

## 📂 Ejecutar la aplicación

- `es/chat/ServidorLanzador.java`: Lanzador del servidor (consola).
- `es/chat/ClienteLanzador.java`: Lanzador del cliente (interfaz gráfica).


## ⬇️ Descarga

- **Clona el repositorio:**
   ```bash
   git clone https://github.com/AdrianGM2001/Chat-Multicliente.git
   ```

## 📷 Imágenes
<p>
    <img src="protocolo/Protocolo.png" alt="Protocolo">
    <img src="img/chat1.png" alt="Imagen de inicio">
    <img src="img/chat2.png" alt="Sesión iniciada">
    <img src="img/chat3.png" alt="Mensajes">
    <img src="img/chat4.png" alt="Chat privado">
    <img src="img/chat5.png" alt="Enviar un mensaje privado">
    <img src="img/chat6.png" alt="Recibir un mensaje privado">
    <img src="img/chat7.png" alt="Abrir un mensaje privado">
    <img src="img/chat8.png" alt="Consola del servidor">
</p>