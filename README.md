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
