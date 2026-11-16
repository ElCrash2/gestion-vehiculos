pipeline {

    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters {

        choice(
            name: 'ACCION',
            choices: ['testeo', 'despliegue'],
            description: 'testeo: tests + imagen + push | despliegue: actualiza servicio Swarm'
        )

        string(
            name: 'TAG_IMAGEN',
            defaultValue: '',
            description: 'Solo despliegue: numero de build publicado en Docker Hub'
        )
    }

    environment {

        // Docker Hub
        DOCKER_HUB_USER = 'iancxt'
        IMAGE_NAME = 'iancxt/gestion-vehiculos'

        // Maven y Java
        MAVEN_IMAGE = 'maven:3.9-eclipse-temurin-21'
        JRE_IMAGE = 'eclipse-temurin:21-jre-alpine'

        MAVEN_CTR = "maven-build-${BUILD_NUMBER}"

        // Docker Swarm
        APP_SERVICE = 'gestion-vehiculos'
        DOCKER_NETWORK = 'RedPostgreSQLNginxTomcat'

        // PostgreSQL
        DB_HOST = 'docker-practicas_postgres'
        DB_PORT = '5432'
        DB_NAME = 'vehiculosdb'
        DB_USER = 'docker'

        // Docker Secrets
        DB_SECRET = 'postgres_password'
        ADMIN_SECRET = 'admin_password'
    }

    stages {

        // =====================================================
        // VALIDACION
        // =====================================================

        stage('Validar parametros') {

            steps {

                script {

                    if (params.ACCION == 'despliegue' &&
                        !params.TAG_IMAGEN?.trim()) {

                        error 'Para despliegue debes indicar TAG_IMAGEN.'
                    }

                    currentBuild.displayName =
                        "#${BUILD_NUMBER} ${params.ACCION}" +
                        (params.ACCION == 'despliegue'
                            ? " (${params.TAG_IMAGEN})"
                            : '')
                }
            }
        }

        // =====================================================
        // TESTS Y EMPAQUETADO MAVEN
        // =====================================================

        stage('Maven: tests + empaquetado') {

            when {
                expression {
                    params.ACCION == 'testeo'
                }
            }

            steps {

                sh '''
                    set -e

                    rm -rf target
                    mkdir -p target

                    docker rm -f "$MAVEN_CTR" >/dev/null 2>&1 || true

                    echo "Iniciando compilacion Maven con Java 21..."

                    docker create \
                        --name "$MAVEN_CTR" \
                        -v maven-repo-cache:/root/.m2 \
                        -w /build \
                        "$MAVEN_IMAGE" \
                        mvn -B -ntp clean verify

                    docker cp . "$MAVEN_CTR":/build

                    set +e
                    docker start -a "$MAVEN_CTR"
                    RC=$?
                    set -e

                    # Recuperar siempre JAR e informes
                    docker cp "$MAVEN_CTR":/build/target/. target/ || true

                    if [ "$RC" -ne 0 ]; then
                        echo "ERROR: La compilacion Maven ha fallado."
                        exit "$RC"
                    fi

                    echo "Compilacion Maven completada."
                '''

                archiveArtifacts(
                    artifacts: 'target/*.jar',
                    fingerprint: true
                )
            }

            post {

                always {

                    junit(
                        allowEmptyResults: true,
                        testResults: 'target/surefire-reports/*.xml'
                    )
                }
            }
        }

        // =====================================================
        // CONSTRUCCION IMAGEN DOCKER
        // =====================================================

        stage('Dockerfile temporal + imagen') {

            when {
                expression {
                    params.ACCION == 'testeo'
                }
            }

            steps {

                sh '''
                    set -e

                    rm -rf docker-build
                    mkdir -p docker-build

                    JAR_COUNT=$(find target \
                        -maxdepth 1 \
                        -type f \
                        -name '*.jar' \
                        ! -name '*-plain.jar' | wc -l)

                    if [ "$JAR_COUNT" -ne 1 ]; then
                        echo "ERROR: Se esperaba un unico JAR ejecutable."
                        find target -maxdepth 1 -name '*.jar'
                        exit 1
                    fi

                    JAR_FILE=$(find target \
                        -maxdepth 1 \
                        -type f \
                        -name '*.jar' \
                        ! -name '*-plain.jar')

                    cp "$JAR_FILE" docker-build/app.jar
                '''

                writeFile(
                    file: 'docker-build/Dockerfile',
                    text: """FROM ${env.JRE_IMAGE}

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY app.jar /app/app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
"""
                )

                sh '''
                    docker build \
                        --label build=${BUILD_NUMBER} \
                        -t "$IMAGE_NAME:$BUILD_NUMBER" \
                        docker-build
                '''
            }
        }

        // =====================================================
        // SMOKE TEST
        // =====================================================

        stage('Smoke test de imagen') {

            when {
                expression {
                    params.ACCION == 'testeo'
                }
            }

            steps {

                sh '''
                    docker run --rm \
                        --label jenkins-temp=${BUILD_NUMBER} \
                        --entrypoint java \
                        "$IMAGE_NAME:$BUILD_NUMBER" \
                        -version
                '''
            }
        }

        // =====================================================
        // PUSH DOCKER HUB
        // =====================================================

        stage('Push a Docker Hub') {

            when {
                expression {
                    params.ACCION == 'testeo'
                }
            }

            steps {

                withCredentials([
                    string(
                        credentialsId: 'docker_hub_token',
                        variable: 'DOCKER_TOKEN'
                    )
                ]) {

                    sh '''
                        set -e

                        export DOCKER_CONFIG="$(mktemp -d)"

                        trap 'docker logout >/dev/null 2>&1 || true; rm -rf "$DOCKER_CONFIG"' EXIT

                        echo "$DOCKER_TOKEN" | docker login \
                            -u "$DOCKER_HUB_USER" \
                            --password-stdin

                        docker push "$IMAGE_NAME:$BUILD_NUMBER"

                        docker logout
                    '''
                }
            }
        }

        // =====================================================
        // DESPLIEGUE DOCKER SWARM
        // =====================================================

        stage('Despliegue Docker Swarm') {

            when {
                expression {
                    params.ACCION == 'despliegue'
                }
            }

            steps {

                withCredentials([
                    string(
                        credentialsId: 'docker_hub_token',
                        variable: 'DOCKER_TOKEN'
                    )
                ]) {

                    sh '''
                        set -eu

                        export DOCKER_CONFIG="$(mktemp -d)"

                        trap 'docker logout >/dev/null 2>&1 || true; rm -rf "$DOCKER_CONFIG"' EXIT

                        NEW_IMAGE="$IMAGE_NAME:$TAG_IMAGEN"

                        echo "=========================================="
                        echo "DESPLIEGUE GESTION DE VEHICULOS"
                        echo "Imagen: $NEW_IMAGE"
                        echo "Servicio: $APP_SERVICE"
                        echo "=========================================="

                        # -------------------------------------
                        # COMPROBAR DOCKER SWARM
                        # -------------------------------------

                        SWARM_STATE=$(docker info \
                            --format '{{.Swarm.LocalNodeState}}')

                        if [ "$SWARM_STATE" != "active" ]; then
                            echo "ERROR: Docker Swarm no esta activo."
                            exit 1
                        fi

                        # -------------------------------------
                        # COMPROBAR RED
                        # -------------------------------------

                        docker network inspect "$DOCKER_NETWORK" >/dev/null

                        echo "Red encontrada: $DOCKER_NETWORK"

                        # -------------------------------------
                        # COMPROBAR DOCKER SECRETS
                        # -------------------------------------

                        docker secret inspect "$DB_SECRET" >/dev/null

                        docker secret inspect "$ADMIN_SECRET" >/dev/null

                        echo "Secret PostgreSQL: OK"
                        echo "Secret ADMIN: OK"

                        # -------------------------------------
                        # COMPROBAR POSTGRESQL
                        # -------------------------------------

                        docker service inspect "$DB_HOST" >/dev/null

                        echo "Servicio PostgreSQL encontrado."

                        # -------------------------------------
                        # DESCARGAR IMAGEN
                        # -------------------------------------

                        echo "$DOCKER_TOKEN" | docker login \
                            -u "$DOCKER_HUB_USER" \
                            --password-stdin

                        docker pull "$NEW_IMAGE"

                        # -------------------------------------
                        # GUARDAR VERSION ANTERIOR
                        # -------------------------------------

                        PREV_IMAGE=""

                        if docker service inspect "$APP_SERVICE" >/dev/null 2>&1; then

                            PREV_IMAGE=$(docker service inspect \
                                -f '{{.Spec.TaskTemplate.ContainerSpec.Image}}' \
                                "$APP_SERVICE")

                            echo "Version anterior: $PREV_IMAGE"

                        else

                            echo "No existe version anterior."
                        fi

                        # -------------------------------------
                        # FUNCION CREAR SERVICIO
                        # -------------------------------------

                        crear_servicio() {

                            IMAGE="$1"

                            echo "Creando servicio con: $IMAGE"

                            docker service create \
                                --name "$APP_SERVICE" \
                                --replicas 1 \
                                --network "$DOCKER_NETWORK" \
                                --restart-condition any \
                                --secret "source=$DB_SECRET,target=postgres_password" \
                                --secret "source=$ADMIN_SECRET,target=admin_password" \
                                --env "DB_HOST=$DB_HOST" \
                                --env "DB_PORT=$DB_PORT" \
                                --env "DB_NAME=$DB_NAME" \
                                --env "DB_USER=$DB_USER" \
                                --with-registry-auth \
                                "$IMAGE"
                        }

                        # -------------------------------------
                        # FUNCION ESPERAR Y COMPROBAR
                        # -------------------------------------

                        esperar() {

                            echo "Esperando arranque de Spring Boot..."

                            for i in $(seq 1 24); do

                                TASK_ID=$(docker service ps \
                                    --no-trunc \
                                    --filter desired-state=running \
                                    -q "$APP_SERVICE" | head -n 1)

                                if [ -n "$TASK_ID" ]; then

                                    CONTAINER_ID=$(docker inspect \
                                        --type task \
                                        -f '{{.Status.ContainerStatus.ContainerID}}' \
                                        "$TASK_ID" 2>/dev/null || true)

                                    if [ -n "$CONTAINER_ID" ] &&
                                       [ "$CONTAINER_ID" != "<no value>" ]; then

                                        # Comprobar respuesta HTTP.
                                        # 200, 301, 302, 401 y 403
                                        # indican que el servidor HTTP responde.

                                        RESPONSE=$(docker exec "$CONTAINER_ID" \
                                            sh -c 'wget -S -O /dev/null http://127.0.0.1:8080/api/vehiculos 2>&1 || true' \
                                            || true)

                                        echo "$RESPONSE" | grep -Eq \
                                            'HTTP/[0-9.]+ (200|301|302|401|403)' && {

                                            echo "Spring Boot responde correctamente."

                                            return 0
                                        }
                                    fi
                                fi

                                echo "Intento $i/24. Esperando aplicacion..."

                                sleep 5
                            done

                            return 1
                        }

                        # -------------------------------------
                        # ELIMINAR SERVICIO ANTERIOR
                        # -------------------------------------

                        echo "Eliminando servicio anterior..."

                        docker service rm "$APP_SERVICE" \
                            >/dev/null 2>&1 || true

                        for i in $(seq 1 20); do

                            if ! docker service inspect "$APP_SERVICE" \
                                >/dev/null 2>&1; then
                                break
                            fi

                            sleep 2
                        done

                        # -------------------------------------
                        # CREAR NUEVO SERVICIO
                        # -------------------------------------

                        echo "Creando nuevo servicio..."

                        crear_servicio "$NEW_IMAGE"

                        # -------------------------------------
                        # COMPROBAR DESPLIEGUE
                        # -------------------------------------

                        if esperar; then

                            echo "=========================================="
                            echo "DESPLIEGUE CORRECTO"
                            echo "=========================================="

                            docker service ls \
                                --filter "name=$APP_SERVICE"

                            docker service ps "$APP_SERVICE"

                        else

                            echo "=========================================="
                            echo "ERROR: EL DESPLIEGUE HA FALLADO"
                            echo "=========================================="

                            docker service ps "$APP_SERVICE" --no-trunc || true

                            # ---------------------------------
                            # MOSTRAR LOGS
                            # ---------------------------------

                            TASK_ID=$(docker service ps \
                                --no-trunc \
                                --filter desired-state=running \
                                -q "$APP_SERVICE" | head -n 1)

                            if [ -n "$TASK_ID" ]; then

                                CONTAINER_ID=$(docker inspect \
                                    --type task \
                                    -f '{{.Status.ContainerStatus.ContainerID}}' \
                                    "$TASK_ID" 2>/dev/null || true)

                                if [ -n "$CONTAINER_ID" ] &&
                                   [ "$CONTAINER_ID" != "<no value>" ]; then

                                    docker logs --tail 100 "$CONTAINER_ID" || true
                                fi
                            fi

                            # ---------------------------------
                            # ROLLBACK
                            # ---------------------------------

                            echo "Iniciando rollback..."

                            docker service rm "$APP_SERVICE" \
                                >/dev/null 2>&1 || true

                            if [ -n "$PREV_IMAGE" ]; then

                                for i in $(seq 1 20); do

                                    if ! docker service inspect "$APP_SERVICE" \
                                        >/dev/null 2>&1; then
                                        break
                                    fi

                                    sleep 2
                                done

                                echo "Recuperando imagen anterior: $PREV_IMAGE"

                                crear_servicio "$PREV_IMAGE"

                                echo "Servicio anterior restaurado."

                            else

                                echo "No existe version anterior para recuperar."
                            fi

                            exit 1
                        fi

                        docker logout
                    '''
                }
            }
        }
    }

    // =====================================================
    // LIMPIEZA
    // =====================================================

    post {

        always {

            script {

                if (params.ACCION == 'testeo') {

                    sh '''
                        docker rm -f "$MAVEN_CTR" \
                            >/dev/null 2>&1 || true

                        docker rmi -f "$IMAGE_NAME:$BUILD_NUMBER" \
                            >/dev/null 2>&1 || true

                        rm -rf docker-build
                    '''
                }
            }
        }

        success {
            echo "Pipeline '${params.ACCION}' completado correctamente."
        }

        failure {
            echo "Pipeline '${params.ACCION}' FALLIDO. Revisa los logs."
        }
    }
}
