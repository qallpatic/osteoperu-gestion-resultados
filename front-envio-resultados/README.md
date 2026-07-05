# Front Envío de Resultados

Portal en **Angular 19** (standalone components + signals) para el envío de resultados de pacientes de OsteoPerú.

## Funcionalidad

- **Login** con guard de ruta y sesión persistida en `localStorage`.
- **Envío de resultados**: número de teléfono (+51, 9 dígitos), DNI (8 dígitos) y carga de archivos **PDF o video** mediante arrastrar/soltar o selección, con validación de tipo y límite de **50 MB por archivo**.
- Diseño responsive con la identidad visual de OsteoPerú.

## Credenciales de acceso

| Usuario                 | Contraseña   |
| ----------------------- | ------------ |
| `osteoperu_resultados`  | `Osteo#2026` |

> Definidas en `src/app/services/auth.service.ts`.

## Comandos

```bash
npm install      # instalar dependencias
npm start        # servidor de desarrollo → http://localhost:4200
npm run build    # build de producción
```

## Estructura

```
src/app/
├─ guards/auth.guard.ts          # protección de rutas
├─ services/auth.service.ts      # autenticación + estado de sesión
├─ pages/login/                  # pantalla de acceso
└─ pages/upload/                 # carga y envío de resultados
```

> El envío está simulado (sin backend). Para integrarlo, reemplaza el `setTimeout`
> de `upload.component.ts` por una llamada `HttpClient` con `FormData`.
