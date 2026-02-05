# 🔍 DIAGNÓSTICO RUNTIME KEYCLOAK

**Fecha:** 2 de febrero de 2026  
**Contenedor:** `keycloak-sso`  
**Imagen:** `quay.io/keycloak/keycloak:25.0.6`  
**Estado:** `Up (unhealthy)`

---

## 📊 RESUMEN EJECUTIVO

| Aspecto              | Estado       | Detalle                                            |
| -------------------- | ------------ | -------------------------------------------------- |
| **Keycloak Runtime** | ✅ FUNCIONAL | Arrancado, escuchando en 8080, realm importado     |
| **Healthcheck**      | ❌ INVÁLIDO  | Comando `curl` no existe en la imagen              |
| **Dependencias**     | ✅ OK        | H2 embebido funcionando                            |
| **Logs**             | ⚠️ WARN      | Event listener RabbitMQ no encontrado (no crítico) |

---

## A. Estado real de Keycloak

- ✅ **Arrancó correctamente**
- ✅ **Arrancó pero healthcheck es inválido**
- ☐ No terminó bootstrap
- ☐ Bloqueado por DB
- ☐ Bloqueado por import

---

## B. Causa raíz EXACTA

> **El contenedor está unhealthy porque el healthcheck intenta ejecutar `curl`, pero este binario NO existe dentro de la imagen oficial de Keycloak 25.0.6.**

---

## C. Evidencia técnica

### Del `docker inspect`:

```json
{
  "Status": "unhealthy",
  "FailingStreak": 67,
  "Log": [
    {
      "Start": "2026-02-02T18:43:17.608904193Z",
      "End": "2026-02-02T18:43:17.687995303Z",
      "ExitCode": -1,
      "Output": "OCI runtime exec failed: exec failed: unable to start container process: exec: \"curl\": executable file not found in $PATH: unknown"
    }
  ]
}
```

**Error consistente en todos los intentos:**

```
exec: "curl": executable file not found in $PATH: unknown
```

### Del `docker logs` (última línea relevante):

```
2026-02-02 18:32:01,576 INFO [io.quarkus] (main) Keycloak 25.0.6 on JVM (powered by Quarkus 3.8.5) started in 6.850s. Listening on: http://0.0.0.0:8080. Management interface listening on http://0.0.0.0:9000.
```

```
2026-02-02 18:32:01,577 INFO [io.quarkus] (main) Profile dev activated.
```

---

## D. Clasificación del problema

- 🔴 **Herramienta faltante (curl / wget)** ← **CAUSA PRINCIPAL**
- 🟡 Healthcheck mal definido (secundario: usa herramienta inexistente)
- ⚪ Endpoint incorrecto para Keycloak 25 (N/A - nunca se ejecutó la petición)
- ⚪ Dependencia externa (DB) (N/A - Keycloak arrancó con H2 embebido)
- ⚪ Configuración Keycloak inválida (N/A - configuración válida)

---

## E. Veredicto final

> **Keycloak está funcional, y Docker lo marca unhealthy porque el healthcheck está configurado para ejecutar `curl`, un binario que NO está incluido en la imagen oficial `quay.io/keycloak/keycloak:25.0`.**

---

## 🔬 ANÁLISIS DETALLADO

### ✅ Evidencia de que Keycloak FUNCIONA:

1. **Bootstrap completado:**
   - Base de datos inicializada (134 changesets aplicados)
   - Realm `videoclub` importado exitosamente
   - Usuario admin creado

2. **Servidor operativo:**
   - Puerto 8080 escuchando (`http://0.0.0.0:8080`)
   - Management interface en puerto 9000 (`http://0.0.0.0:9000`)
   - Profile `dev` activado

3. **Sin errores críticos:**
   - Realm import exitoso (IGNORE_EXISTING strategy)
   - No hay fallos de conectividad con DB
   - No hay stacktraces de errores fatales

### ❌ Por qué falla el healthcheck:

**FailingStreak:** 67 intentos consecutivos fallidos

**Todos los logs muestran el mismo error:**

```
exec: "curl": executable file not found in $PATH
```

**Implicación:**

- La imagen oficial de Keycloak 25.0 **NO incluye curl** en su filesystem
- El contenedor nunca pudo ejecutar el comando de health verification
- Docker marca como `unhealthy` porque el healthcheck retorna `-1`

---

## 📋 LOGS COMPLETOS DE STARTUP

```
2026-01-29 21:54:28,540 INFO  [io.qua.dep.QuarkusAugmentor] (main) Quarkus augmentation completed in 11674ms
2026-01-29 21:54:31,830 INFO  [org.infinispan.CONTAINER] (ForkJoinPool.commonPool-worker-1) ISPN000556: Starting user marshaller 'org.infinispan.jboss.marshalling.core.JBossUserMarshaller'
2026-01-29 21:54:32,794 INFO  [org.keycloak.quarkus.runtime.storage.legacy.liquibase.QuarkusJpaUpdaterProvider] (main) Initializing database schema. Using changelog META-INF/jpa-changelog-master.xml

UPDATE SUMMARY
Run:                        134
Previously run:               0
Filtered out:                 0
-------------------------------
Total change sets:          134

2026-01-29 21:54:34,788 INFO  [org.keycloak.connections.infinispan.DefaultInfinispanConnectionProviderFactory] (main) Node name: node_437366, Site name: null
2026-01-29 21:54:34,920 INFO  [org.keycloak.broker.provider.AbstractIdentityProviderMapper] (main) Registering class org.keycloak.broker.provider.mappersync.ConfigSyncEventListener
2026-01-29 21:54:34,965 INFO  [org.keycloak.services] (main) KC-SERVICES0050: Initializing master realm
2026-01-29 21:54:36,098 INFO  [org.keycloak.exportimport.singlefile.SingleFileImportProvider] (main) Full importing from file /opt/keycloak/bin/../data/import/realm-export.json
2026-01-29 21:54:37,765 INFO  [org.keycloak.exportimport.util.ImportUtils] (main) Realm 'videoclub' imported
2026-01-29 21:54:37,793 INFO  [org.keycloak.exportimport.dir.DirImportProvider] (main) Importing from directory /opt/keycloak/bin/../data/import
2026-01-29 21:54:37,793 INFO  [org.keycloak.services] (main) KC-SERVICES0030: Full model import requested. Strategy: IGNORE_EXISTING
2026-01-29 21:54:37,793 INFO  [org.keycloak.services] (main) KC-SERVICES0032: Import finished successfully
2026-01-29 21:54:37,860 INFO  [org.keycloak.services] (main) KC-SERVICES0009: Added user 'admin' to realm 'master'
2026-01-29 21:54:37,915 INFO  [io.quarkus] (main) Keycloak 25.0.6 on JVM (powered by Quarkus 3.8.5) started in 9.156s. Listening on: http://0.0.0.0:8080. Management interface listening on http://0.0.0.0:9000.
2026-01-29 21:54:37,915 INFO  [io.quarkus] (main) Profile dev activated.
2026-01-29 21:54:37,916 INFO  [io.quarkus] (main) Installed features: [agroal, cdi, hibernate-orm, jdbc-h2, keycloak, logging-gelf, narayana-jta, reactive-routes, resteasy-reactive, resteasy-reactive-jackson, smallrye-context-propagation, vertx]
2026-01-29 21:54:37,920 WARN  [org.keycloak.quarkus.runtime.KeycloakMain] (main) Running the server in development mode. DO NOT use this configuration in production.
```

---

## 🎯 CONCLUSIÓN

**Keycloak está completamente funcional pero Docker lo marca como unhealthy debido a un healthcheck mal configurado que intenta ejecutar un binario inexistente (`curl`).**

### Siguiente paso recomendado:

**PATCH DE HEALTHCHECK** utilizando una de estas alternativas:

1. Usar el management endpoint nativo de Keycloak en puerto 9000
2. Cambiar a verificación basada en socket (test -f o nc)
3. Instalar curl en una imagen custom derivada
4. Eliminar el healthcheck si no es crítico

---

**Estado:** ✅ Diagnóstico completado - Pendiente de solución
