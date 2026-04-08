# SquadUp Backend

Bienvenido al repositorio Backend de **SquadUp**. 
SquadUp es una plataforma social para conectar gamers en línea, crear lobbies competitivos y fomentar la interacción en tiempo real. 

## 📖 Documentación

Hemos documentado todo el proyecto exhaustivamente dentro del directorio `/docs`. Por favor, revisa la siguiente documentación para entender la arquitectura y empezar a contribuir:

1. [Visión General y Arquitectura](docs/1-overview.md)
2. [Esquema de Base de Datos y Entidades](docs/2-database-schema.md)
3. [Referencia de la API (Endpoints)](docs/3-api-reference.md)
4. [Seguridad (JWT) y WebSockets (Chat en vivo)](docs/4-security-and-websockets.md)
5. [Log de Cambios y Commits](docs/5-changelog.md)

## 🚀 Requisitos para entorno local

1. **Java 17** y **Maven 3.x**.
2. **MySQL 8+** (Asegúrate de que el servicio esté corriendo en el puerto 3306).
3. Base de datos `SquatUp1` u otra (modificar las variables de entorno).

## ⚙️ Correr el proyecto
```bash
# Iniciar la aplicación en el perfil default (Local)
mvn spring-boot:run
```
La API estará disponible en `http://localhost:8080/api/`
