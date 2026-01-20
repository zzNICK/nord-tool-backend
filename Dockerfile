# Imagem base com Java
FROM eclipse-temurin:11-jre-alpine

# Diretório de trabalho dentro do container
WORKDIR /app

# Copia o jar gerado para dentro do container
COPY target/*.jar app.jar

# Porta que a aplicação expõe
EXPOSE 8081

# Comando para subir a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
