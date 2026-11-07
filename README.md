# Lifting Club Fitness — Sistema de gestión de gimnasio

Aplicación web para administrar un gimnasio: clientes, entrenadores, membresías y pagos,
asistencia, rutinas, registro físico, reportes y usuarios con roles.

Proyecto académico de Ingeniería de Sistemas (Universidad del Tolima), migrado de una versión
de escritorio (Swing) a web.

## Tecnologías

- Java 17+ y Spring Boot (Spring MVC)
- Thymeleaf (vistas HTML del lado del servidor)
- MySQL 8+ con JDBC (patrón DAO + DTO)
- Maven
- HTML, CSS y JavaScript propios (sin frameworks de interfaz); gráficos con Chart.js

## Módulos

| Módulo | Qué hace |
|---|---|
| Inicio | Resumen del día: asistencias, membresías vigentes / por vencer / vencidas, ingresos del mes |
| Clientes, Entrenadores, Administradores | Altas, ediciones, bajas y foto |
| Membresías | Estado, renovación y registro del pago |
| Asistencia | Entradas al gimnasio por cliente, entrenador y turno |
| Rutinas | Planes de entrenamiento con ejercicios, versión para imprimir |
| Registro físico | Peso, altura e IMC por cliente |
| Reportes | Gráficos de ingresos, membresías, género, edades y turnos |
| Usuarios | Cuentas de acceso con roles (solo administrador) |

### Roles

| Rol | Módulos |
|---|---|
| Administrador | Todos |
| Recepción | Clientes, membresías, asistencia, registro físico |
| Entrenador | Asistencia, rutinas, registro físico |

## Cómo ejecutarlo en tu computador

### 1. Requisitos
- JDK 17 o superior
- Maven
- MySQL 8 o superior

### 2. Crear la base de datos
Abre `gimnasio_db_v2.sql` en MySQL Workbench (File > Open SQL Script) y ejecútalo completo.
Crea la base `gimnasio_db`, las tablas, los tipos de membresía y un usuario inicial.

### 3. Configurar la conexión
Por defecto la aplicación se conecta a `localhost:3306/gimnasio_db` con el usuario `root`.
Se puede cambiar con variables de entorno, sin tocar el código:

| Variable | Valor por defecto |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/gimnasio_db` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | (la definida en `util/ConexionBD.java`, solo para desarrollo local) |

Ejemplo en macOS / Linux:

```bash
export DB_PASSWORD="mi_clave"
mvn spring-boot:run
```

### 4. Ejecutar
```bash
mvn spring-boot:run
```
Abre http://localhost:8080

### 5. Primer ingreso
Usuario inicial: **admin** / contraseña **admin123**.
**Cámbiala apenas entres** (Usuarios > Contraseña). Desde ahí puedes crear los demás usuarios.

## Notas de seguridad

- Las contraseñas se guardan con salt + SHA-256, nunca en texto plano.
- El acceso está protegido por sesión y por rol; el inicio de sesión se bloquea temporalmente tras varios intentos fallidos.
- Los formularios no usan tokens CSRF (la cookie de sesión usa `SameSite=Lax`). Para un uso real en producción se recomienda migrar a Spring Security.
- Las zonas horarias se manejan en `America/Bogota`, de modo que las fechas son correctas aunque el servidor esté en UTC.

## Despliegue en Vercel (rama `deploy/vercel-serverless`)

Esta rama empaqueta la aplicación como una imagen de contenedor y la publica con el
[Container Runtime](https://vercel.com/docs/functions/runtimes/container) de Vercel
(beta, disponible en el plan Hobby). No hay runtime de JVM nativo en Vercel: el
`Dockerfile` de la raíz es lo que Vercel construye y enruta.

### Qué hace falta antes de desplegar

1. **Una base de datos MySQL accesible desde internet.** Vercel no ofrece MySQL.
   Sirve PlanetScale, Aiven, Railway (Postgres no; tiene que ser MySQL) u otro host.
   Ejecuta `gimnasio_db_v2.sql` ahí y anota host, usuario y clave.
2. **Una cuenta de Vercel** y el CLI (`npm i -g vercel`) o un `VERCEL_TOKEN`.
3. Definir estas variables de entorno en el proyecto de Vercel
   (Settings → Environment Variables), para Production y Preview:

   | Variable | Ejemplo |
   |---|---|
   | `DB_URL` | `jdbc:mysql://HOST:3306/gimnasio_db?useSSL=true&serverTimezone=America/Bogota` |
   | `DB_USER` | usuario de la base |
   | `DB_PASSWORD` | clave de la base |
   | `PORT` | `8080` (Vercel lo inyecta; déjalo explícito si el panel lo pide) |

### Cómo desplegar

```bash
vercel link          # vincula este directorio a un proyecto de Vercel
vercel env add DB_URL
vercel env add DB_USER
vercel env add DB_PASSWORD
vercel deploy        # preview
vercel deploy --prod # producción
```

El primer despliegue de un proyecto nuevo cuenta como Production. Usa credenciales
de prueba en ese primer intento.

### Limitaciones de este despliegue (importante para la presentación)

- **Sesiones en memoria.** La app guarda la sesión en la memoria del proceso
  (`HttpSession` de Tomcat, sin Spring Session). En Vercel la instancia se apaga
  tras 5 minutos sin tráfico en producción (30 segundos en preview) y el siguiente
  request puede caer en otra instancia. **El usuario puede tener que volver a
  iniciar sesión.** Para un uso real haría falta Spring Session con Redis.
- **Arranque en frío.** La primera petición después de un periodo inactivo espera
  a que arranque la JVM y Spring (varios segundos).
- **Plan Hobby.** 2 GB / 1 vCPU y 300 s de duración máxima por invocación. El
  contenedor cabe, pero no hay margen para cargas grandes.
- **Contraseña inicial.** El usuario sembrado es `admin` / `admin123`. Cámbiala
  en cuanto entres (Usuarios → Contraseña) antes de mostrar la app en público.
