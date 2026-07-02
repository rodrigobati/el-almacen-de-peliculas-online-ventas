workspace "Vertical Ventas" "Arquitectura C4 completa de la vertical Ventas de El Almacen de Peliculas Online" {

    !identifiers hierarchical

    model {
        cliente = person "Cliente" "Persona autenticada que administra su carrito y realiza compras."

        frontend = softwareSystem "Frontend Web" "Aplicacion React desde la que el cliente administra el carrito y revisa sus compras." {
            tags "Web Application"
        }

        apiGateway = softwareSystem "API Gateway" "Punto de entrada que enruta las solicitudes HTTP hacia las verticales."

        keycloak = softwareSystem "Keycloak" "Proveedor de identidad que autentica al cliente y emite tokens JWT." {
            tags "Identity Provider"
        }

        catalogo = softwareSystem "Vertical Catalogo" "Administra las peliculas y su stock."

        descuentos = softwareSystem "Vertical Descuentos" "Valida cupones y devuelve el descuento aplicable."

        notificaciones = softwareSystem "Vertical Notificaciones" "Procesa compras confirmadas y envia las notificaciones correspondientes."

        rabbitmq = softwareSystem "RabbitMQ" "Broker compartido para eventos de peliculas, stock, compras y validacion de cupones." {
            tags "Message Broker"
        }

        ventas = softwareSystem "Vertical Ventas" "Gestiona el carrito, la confirmacion de compras, el historial y la coordinacion del stock." {
            tags "Ventas"

            api = container "API de Ventas" "Expone las operaciones de carrito y compras, mantiene una proyeccion del catalogo y coordina la confirmacion asincrona." "Java 21, Spring Boot 4" {
                tags "Ventas"

                security = component "Seguridad JWT" "Autentica las solicitudes, valida el token y obtiene la identidad del cliente." "Spring Security, OAuth 2.0 Resource Server" {
                    tags "Security"
                }

                controllers = component "Controladores REST" "Expone los endpoints de carrito, compras, stock y reconstruccion de proyecciones." "Spring MVC" {
                    tags "Controller"
                }

                application = component "Servicios de Aplicacion" "Orquesta los casos de uso de carrito, confirmacion, aceptacion y compensacion de compras." "Spring Services" {
                    tags "Service"
                }

                domain = component "Modelo de Dominio" "Contiene las reglas de Carrito, PeliculaEnCarrito, Compra, DetalleCompra y Descuento." "Java" {
                    tags "Domain"
                }

                persistence = component "Adaptadores de Persistencia" "Implementa repositorios JPA y JDBC para carritos, compras, proyecciones, outbox e idempotencia." "Spring Data JPA, JDBC" {
                    tags "Repository"
                }

                catalogAdapter = component "Adaptador de Catalogo" "Obtiene peliculas por HTTP para reconstruir la proyeccion local." "Java HTTP Client, JSON"

                discountAdapter = component "Adaptador de Descuentos" "Valida cupones mediante una llamada RPC sobre RabbitMQ." "Spring AMQP RPC"

                messaging = component "Mensajeria y Outbox" "Publica solicitudes de stock y compras confirmadas; consume cambios de peliculas y resultados de stock." "Spring AMQP, Transactional Outbox" {
                    tags "Messaging"
                }
            }

            database = container "Base de Datos de Ventas" "Persiste carritos, compras, items, proyeccion de peliculas, eventos procesados y outbox." "MySQL" {
                tags "Database"
            }
        }

        cliente -> ventas "Administra el carrito, confirma compras y consulta su historial"
        ventas -> keycloak "Valida la identidad y autorizacion del cliente" "OAuth 2.0 / JWT"
        ventas -> catalogo "Sincroniza peliculas y coordina la validacion de stock"
        ventas -> descuentos "Solicita la validacion de cupones"
        ventas -> notificaciones "Emite compras confirmadas para su notificacion"

        cliente -> frontend "Usa" "HTTPS"
        frontend -> keycloak "Inicia sesion y obtiene tokens" "OIDC / OAuth 2.0"
        frontend -> apiGateway "Envia operaciones de carrito y compras" "HTTPS / JSON"
        apiGateway -> ventas.api "Enruta las solicitudes de Ventas" "HTTP / JSON"

        ventas.api -> ventas.database "Lee y persiste carritos, compras, proyecciones y eventos" "JDBC / JPA"
        ventas.api -> keycloak "Obtiene claves publicas para validar tokens" "HTTPS / JWKS"
        ventas.api -> catalogo "Reconstruye la proyeccion local de peliculas" "HTTP / JSON"
        ventas.api -> rabbitmq "Publica y consume mensajes de integracion" "AMQP"
        rabbitmq -> catalogo "Entrega solicitudes de validacion de stock" "AMQP"
        catalogo -> rabbitmq "Publica cambios de peliculas y resultados de stock" "AMQP"
        rabbitmq -> descuentos "Solicita la validacion de cupones y retorna la respuesta RPC" "AMQP / RPC"
        rabbitmq -> notificaciones "Entrega eventos de compra confirmada" "AMQP"

        apiGateway -> ventas.api.security "Envia solicitudes protegidas" "HTTP / JWT"
        ventas.api.security -> keycloak "Obtiene las claves publicas" "HTTPS / JWKS"
        ventas.api.security -> ventas.api.controllers "Autoriza la ejecucion de los endpoints"
        ventas.api.controllers -> ventas.api.application "Invoca los casos de uso"
        ventas.api.application -> ventas.api.domain "Ejecuta las reglas de negocio"
        ventas.api.application -> ventas.api.persistence "Carga y persiste el estado"
        ventas.api.application -> ventas.api.catalogAdapter "Solicita la reconstruccion de la proyeccion"
        ventas.api.application -> ventas.api.discountAdapter "Solicita la validacion de cupones"
        ventas.api.application -> ventas.api.messaging "Registra eventos en el outbox"
        ventas.api.persistence -> ventas.database "Lee y escribe" "JPA / JDBC"
        ventas.api.catalogAdapter -> catalogo "Consulta todas las peliculas" "HTTP / JSON"
        ventas.api.discountAdapter -> rabbitmq "Realiza la validacion RPC de cupones" "AMQP / RPC"
        ventas.api.messaging -> rabbitmq "Publica solicitudes de stock y compras confirmadas" "AMQP"
        rabbitmq -> ventas.api.messaging "Entrega cambios de peliculas y resultados de stock" "AMQP"
        ventas.api.messaging -> ventas.api.application "Notifica aceptaciones o rechazos de stock"

        codeCarritoController = element "CarritoController" "Clase Java" "Expone las operaciones para ver y modificar el carrito." {
            tags "Code Controller"
        }

        codeCompraController = element "CompraController" "Clase Java" "Expone confirmacion, historial y detalle de compras." {
            tags "Code Controller"
        }

        codeCarritoService = element "CarritoService" "Clase Java" "Implementa los casos de uso de administracion del carrito." {
            tags "Code Service"
        }

        codeConfirmarCompraService = element "ConfirmarCompraService" "Clase Java" "Confirma la compra, aplica descuentos y registra la validacion de stock en el outbox." {
            tags "Code Service"
        }

        codeCarrito = element "Carrito" "Clase de dominio" "Agregado raiz que protege las reglas del carrito y crea una Compra." {
            tags "Code Domain"
        }

        codePeliculaEnCarrito = element "PeliculaEnCarrito" "Clase de dominio" "Entidad que mantiene pelicula, precio y cantidad dentro del carrito." {
            tags "Code Domain"
        }

        codeCompra = element "Compra" "Clase de dominio" "Representa una compra confirmada con sus importes y detalles." {
            tags "Code Domain"
        }

        codeCarritoRepository = element "CarritoRepository" "Interfaz Java" "Puerto de persistencia para cargar y guardar el agregado Carrito." {
            tags "Code Interface"
        }

        codeJpaCarritoRepository = element "JpaCarritoRepository" "Clase Java" "Adaptador que implementa CarritoRepository sobre Spring Data JPA." {
            tags "Code Repository"
        }

        codeCompraJpaRepository = element "CompraJpaRepository" "Interfaz Spring Data" "Repositorio de entidades de compras e historial del cliente." {
            tags "Code Repository"
        }

        codeOutboxEventService = element "OutboxEventService" "Clase Java" "Registra eventos de integracion en la misma transaccion de la compra." {
            tags "Code Integration"
        }

        codeDescuentosRpcClient = element "DescuentosRpcClient" "Clase Java" "Cliente RPC que solicita la validacion de cupones." {
            tags "Code Integration"
        }

        codeCarritoController -> codeCarritoService "delega en"
        codeCompraController -> codeConfirmarCompraService "delega en"
        codeCarritoService -> codeCarritoRepository "usa"
        codeCarritoService -> codeCarrito "modifica"
        codeConfirmarCompraService -> codeCarritoRepository "usa"
        codeConfirmarCompraService -> codeCompraJpaRepository "persiste mediante"
        codeConfirmarCompraService -> codeOutboxEventService "registra eventos mediante"
        codeConfirmarCompraService -> codeDescuentosRpcClient "valida cupones mediante"
        codeConfirmarCompraService -> codeCarrito "confirma"
        codeJpaCarritoRepository -> codeCarritoRepository "implementa"
        codeJpaCarritoRepository -> codeCarrito "mapea y persiste"
        codeCarrito -> codePeliculaEnCarrito "contiene"
        codeCarrito -> codeCompra "crea"
    }

    views {
        properties {
            "structurizr.sort" "key"
        }

        systemContext ventas "01_Ventas_Contexto" {
            title "C4 Nivel 1 - Contexto de la Vertical Ventas"
            include cliente ventas keycloak catalogo descuentos notificaciones
            autoLayout tb 350 250
            default
        }

        container ventas "02_Ventas_Contenedores" {
            title "C4 Nivel 2 - Contenedores de la Vertical Ventas"
            include *
            include cliente frontend descuentos notificaciones
            autoLayout tb 350 250
        }

        component ventas.api "03_Ventas_Componentes" {
            title "C4 Nivel 3 - Componentes de la API de Ventas"
            include *
            include descuentos notificaciones
            autoLayout tb 350 250
        }

        custom "04_Ventas_Codigo" {
            title "C4 Nivel 4 - Codigo principal de la Vertical Ventas"
            description "Vista personalizada de clases; Structurizr no ofrece una vista de codigo C4 nativa."
            include codeCarritoController codeCompraController
            include codeCarritoService codeConfirmarCompraService
            include codeCarrito codePeliculaEnCarrito codeCompra
            include codeCarritoRepository codeJpaCarritoRepository codeCompraJpaRepository
            include codeOutboxEventService codeDescuentosRpcClient
            autoLayout tb 300 220
        }

        styles {
            element "Element" {
                color #ffffff
                stroke #7f1d1d
                strokeWidth 3
                shape roundedbox
            }

            element "Person" {
                background #08427b
                stroke #052e56
                shape person
            }

            element "Software System" {
                background #6b7280
                stroke #374151
            }

            element "Ventas" {
                background #d9232b
                stroke #991b1b
            }

            element "Container" {
                background #dc2626
                stroke #991b1b
            }

            element "Component" {
                background #e11d48
                stroke #881337
                shape component
            }

            element "Database" {
                background #7f1d1d
                stroke #450a0a
                shape cylinder
            }

            element "Web Application" {
                background #2563eb
                stroke #1e3a8a
                shape webBrowser
            }

            element "Identity Provider" {
                background #7c3aed
                stroke #4c1d95
                shape hexagon
            }

            element "Message Broker" {
                background #f59e0b
                color #111827
                stroke #92400e
                shape pipe
            }

            element "Security" {
                background #7c3aed
                stroke #4c1d95
            }

            element "Domain" {
                background #15803d
                stroke #14532d
            }

            element "Repository" {
                background #0369a1
                stroke #0c4a6e
            }

            element "Messaging" {
                background #d97706
                stroke #78350f
            }

            element "Code Controller" {
                background #2563eb
                stroke #1e3a8a
                shape component
            }

            element "Code Service" {
                background #d9232b
                stroke #991b1b
                shape component
            }

            element "Code Domain" {
                background #15803d
                stroke #14532d
                shape roundedbox
            }

            element "Code Interface" {
                background #7c3aed
                stroke #4c1d95
                shape roundedbox
            }

            element "Code Repository" {
                background #0369a1
                stroke #0c4a6e
                shape cylinder
            }

            element "Code Integration" {
                background #d97706
                stroke #78350f
                shape component
            }

            element "Boundary" {
                stroke #991b1b
                strokeWidth 4
            }

            relationship "Relationship" {
                color #4b5563
                thickness 3
                routing orthogonal
            }
        }
    }

    configuration {
        scope softwaresystem
    }
}
