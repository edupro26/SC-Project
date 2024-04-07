# SC | Projeto 1 - Fase 2 | 2023/24


<br> Projeto desenvolvido no âmbito da disciplina Seguranca e Confiabilidade
da Faculdade de Ciências da Universidade de Lisboa.


### Autores 


Grupo 6:

- Eduardo Proenca - 57551
- Manuel Barral - 52026
- Tiago Oliveira - 54979


## Descrição
 

O projeto implementa um sistema de IoT, com uma arquitetura cliente-servidor. 
Este é composto por dois programas que comunicam entre si, o programa cliente 
`IoTDevice` e o programa servidor `IoTServer`.


### Estrutura do projeto


O projeto encontra-se organizado com uma estrutura maven, composta por dois
módulos, `IoTDevice` e `IoTServer`.

> ##### Módulo *IoTDevice*:
> - `src/main/java/client` contém código fonte do **IoTDevice**

> ##### Módulo *IoTServer*:
> - `src/main/java/server` contém código fonte do **IoTSever**
> - `src/main/resources` contém recursos utilizados pelo servidor

**Nota:**

Durante a comunicação entre cliente e servidor serão criados os seguintes diretórios:

- `server-output` contém ficheiros enviados do servidor para o cliente
- `server-files` contém ficheiros com dados do servidor
- `temperatures` contém ficheiros com as temperaturas dos dispositivos em cada domínio do servidor
- `images` contém as imagens enviadas do cliente para o servidor 



### Funcionalidades


O programa `IoTDevice` disponibiliza ao utilizador a seguinte interface de comandos.


> ##### Lista de comandos:
> - `CREATE <dm>` - criar um novo domínio com o nome **dm** no servidor.
> - `ADD <user> <dm>` -  adiciona um o **user** ao domínio **dm**
> - `RD <dm>` - regista o dispositivo atual no domínio **dm**
> - `ET <float>` - envia ao servidor o valor de temperatura **float**
> - `EI <filename.jpg>` - envia ao servidor a imagem **filename.jpg**
> - `RT <dm>` - pede ao servidor um ficheiro com as temperaturas de cada dispositivo no domínio **dm**
> - `RI <user>:<dev_id>` - pede ao servidor a imagem correspondente ao dispositivo **user:dev_id**


**Notas:** 

- O servidor apenas suporta imagens com a extenção .jpg
- Todas as funcionalidades pedidas no enuciado do projeto foram implementadas


## Compilação e Execução


Para compilar e executar o projeto, siga os passos abaixo:


1. Dentro do diretório do projeto executar o script `build.sh`

         $ ./build.sh
         
         
         Conteudo do script:

         # Build Server
         javac -d out/server/classes -cp IoTServer/src/main/java IoTServer/src/main/java/server/*.java
         cp -r IoTServer/src/main/resources/* out/server/classes
         jar cvfe out/IoTServer.jar server.IoTServer -C out/server/classes .

         # Build Client
         javac -d out/client/classes -cp IoTDevice/src/main/java IoTDevice/src/main/java/client/*.java
         jar cvfe out/IoTDevice.jar client.IoTDevice -C out/client/classes .

2. Dentro do diretório `out`, executar o seguinte comando para iniciar o servidor

         $ java -jar IoTServer.jar <port>

3. Dentro do diretório `out`, executar o seguinte comando para iniciar o cliente

         $ java -jar IoTDevice.jar <IP/hostname>[:Port] <dev-id> <user-id>

Caso pertenda apagar a pasta `out` pode executar o script `clean.sh`.


**Notas:** <br>

- Para utilizar mais do que um cliente, execute o comando para iniciar o cliente em diferentes terminais
- Pode também utilizar o maven como build tool, executando o comando `mvn package`. Note que, terá de 
atualizar o ficheiro `device_info.csv` localizado dentro do diretório `src/main/resources` do módulo
`IoTServer`, com o nome e tamanho corretos do arquivo .jar do cliente

## Chaves

Comando para gerar chave assimétrica:

```bash
keytool -genkeypair -alias IoTServerKeyPair -keyalg RSA -keysize 2048 -keystore keystore.server
```