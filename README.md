# SC - Projeto 1 (2023/2024)


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
> - `target` contém o arquivo .jar do cliente, gerado pelo maven

> ##### Módulo *IoTServer*:
> - `src/main/java/server` contém código fonte do **IoTSever**
> - `src/main/resources` contém recursos utilizados pelo servidor
> - `target` contém o arquivo .jar do servidor, gerado pelo maven

**Nota:**

Durante a comunicação entre cliente e servidor serão criados os seguintes diretórios:

- `server-output` criado do lado do cliente. Contém ficheiros enviados do servidor para
o cliente
- `server-files` criado do lado do servidor. Contém ficheiros que dados do servidor
- `temperatures` criado do lado do servidor. Contém ficheiros com as temperaturas dos
dispositivos em cada domínio do servidor
- `images` criado do lado do servidor. Contém as imagens enviadas do cliente para o servidor 



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
- Todas as funcionalidades pedidas no enuciado do projeto foram implementadas.


## Compilação e Execução


Como referido anteriormente, o projeto está organizado com uma estrutura maven, sendo que utiliza
o mesmo como build tool. Desta maneira, pode utilizar o comando `mvn package` para compilar
e compactar o código fonte em dois arquivos *.jar*, um para o cliente e outro para o servidor.
Estes arquivos serão guardados nos diretórios `target` correspondentes.

De seguida, para executar o projeto, siga estes passos:

1. Executar o seguinte comando no diretório `IoTServer/target` para iniciar o servidor

         $ java -jar IoTServer-grupo6.jar <port>

2. Executar seguinte o comando no diretório `IoTDevice/target` para iniciar o cliente

         $ java -jar IoTDevice-grupo6.jar <IP/hostname>[:Port] <dev-id> <user-id>

Caso pertenda apagar as pastas `target` pode utilizar o comando `mvn clean`.

<br>

Alternativamente, se não pertender utilizar o maven, pode seguir os seguintes passos para
compilar e executar o projeto:

1. Dentro do diretório do projeto executar o script `build.sh`

         $ bash build.sh

2. Dentro do diretório `out`, executar o seguinte comando para iniciar o servidor

         $ java -jar IoTServer.jar <port>

3. Dentro do diretório `out`, executar o seguinte comando para iniciar o cliente

         $ java -jar IoTDevice.jar <IP/hostname>[:Port] <dev-id> <user-id>

Caso pertenda apagar as pasta `out` pode executar o script `clean.sh`.


**Notas:** <br>

- Para iniciar mais do que um cliente, execute o comando de execução do cliente em diferentes terminais
- Caso o servidor continuar a responder ***NOK-TESTED*** depois de autenticar o utilizador e validar o 
id do dispositivo, verifique se o tamanho do arquivo .jar do cliente corresponde a um dos tamanhos (em bytes)
no ficheiro **device_info.csv** no diretório `src/main/resources` do módulo `IoTServer`.