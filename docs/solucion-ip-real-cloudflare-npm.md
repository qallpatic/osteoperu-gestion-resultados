# Restaurar la IP real del usuario con Cloudflare Tunnel y Nginx Proxy Manager

## Contexto

Arquitectura desplegada con Docker:

```text
Usuario
  → Cloudflare
  → contenedor cloudflared
  → Nginx Proxy Manager
  → Nginx del frontend Angular
  → backend Spring Boot
```

El frontend Angular está servido por un Nginx interno y este reenvía las solicitudes de API al backend Spring Boot.

## Problema

El backend recibía como IP del cliente la dirección IP interna del contenedor `cloudflared`, en lugar de la IP pública real del usuario.

Esto impedía que el sistema de bloqueo por intentos fallidos funcionara correctamente por dirección IP.

## Solución aplicada

No fue necesario modificar:

- El Nginx interno que sirve Angular.
- La configuración del backend Spring Boot.
- Las ubicaciones personalizadas de Nginx Proxy Manager.

El único cambio necesario fue agregar la siguiente configuración en:

```text
Nginx Proxy Manager
→ Proxy Hosts
→ Edit Proxy Host
→ Advanced
→ Custom Nginx Configuration
```

Configuración aplicada:

```nginx
# Solo se acepta CF-Connecting-IP cuando la conexión proviene
# del contenedor cloudflared.
set_real_ip_from 172.30.0.19;

# Cloudflare envía la IP original del usuario en este encabezado.
real_ip_header CF-Connecting-IP;

# Permite resolver correctamente la IP en una cadena de proxies.
real_ip_recursive on;

# Reenvía la IP normalizada hacia los siguientes servicios.
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $remote_addr;

# Conserva el protocolo y host originales utilizados por el cliente.
proxy_set_header X-Forwarded-Proto $http_x_forwarded_proto;
proxy_set_header X-Forwarded-Host $host;
```

## Por qué funcionó

Cloudflare Tunnel entrega la solicitud a Nginx Proxy Manager desde la IP interna del contenedor `cloudflared`.

Sin embargo, Cloudflare incluye la IP pública original del visitante en:

```http
CF-Connecting-IP
```

Esta configuración hace que Nginx Proxy Manager:

1. Confíe en `CF-Connecting-IP` únicamente cuando la solicitud provenga de `172.30.0.19`.
2. Reemplace internamente `$remote_addr` por la IP real del visitante.
3. Envíe esa IP a los siguientes proxies mediante `X-Real-IP` y `X-Forwarded-For`.
4. Permita que la configuración existente del Nginx frontend continúe propagando la IP hacia Spring Boot.

## Resultado

Después del cambio, el backend comenzó a recibir la IP real del usuario y el sistema de bloqueo por intentos fallidos volvió a funcionar correctamente.

## Consideración importante sobre Docker

La configuración depende actualmente de esta IP:

```text
172.30.0.19
```

Si el contenedor `cloudflared` se recrea, Docker podría asignarle otra IP.

Para verificar la IP actual:

```bash
docker inspect cloudflared   --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'
```

El nombre `cloudflared` debe reemplazarse si el contenedor utiliza otro nombre.

### Recomendación

Asignar una IP estática al contenedor `cloudflared` dentro de una red Docker definida explícitamente.

Ejemplo:

```yaml
services:
  cloudflared:
    image: cloudflare/cloudflared:latest
    networks:
      proxy-network:
        ipv4_address: 172.30.0.19

networks:
  proxy-network:
    ipam:
      config:
        - subnet: 172.30.0.0/24
```

Antes de aplicarlo, se debe comprobar que la subred y la IP no estén utilizadas por otros contenedores.

## Seguridad

No se debe reemplazar:

```nginx
set_real_ip_from 172.30.0.19;
```

por:

```nginx
set_real_ip_from 0.0.0.0/0;
```

Confiar en cualquier origen permitiría falsificar `CF-Connecting-IP` y evadir o provocar bloqueos por IP.

También se recomienda que:

- Nginx Proxy Manager solo sea alcanzable mediante `cloudflared`, cuando esa sea la arquitectura prevista.
- El backend Spring Boot no publique directamente su puerto hacia Internet.
- La red Docker limite qué contenedores pueden comunicarse con el backend.

## Procedimiento de recuperación

Si después de recrear los contenedores el backend vuelve a recibir una IP interna:

1. Consultar la IP actual del contenedor `cloudflared`.
2. Compararla con la configurada en `set_real_ip_from`.
3. Actualizar la configuración avanzada del Proxy Host si cambió.
4. Guardar los cambios en Nginx Proxy Manager.
5. Probar nuevamente un inicio de sesión y revisar la IP recibida por Spring Boot.

## Fecha y experiencia registrada

Solución validada en una arquitectura con:

- Angular 19.
- Spring Boot.
- Cloudflare Tunnel ejecutado como contenedor.
- Nginx Proxy Manager.
- Nginx interno en el contenedor del frontend.
- Docker.
