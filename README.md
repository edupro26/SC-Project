# SC | Projeto 1 - Fase 2 | 2023/24


<br> Projeto desenvolvido no âmbito da disciplina Seguranca e Confiabilidade
da Faculdade de Ciências da Universidade de Lisboa.


### Autores 


Grupo 6:

- Eduardo Proenca - 57551
- Manuel Barral - 52026
- Tiago Oliveira - 54979


## Descrição
 

O projeto implementa um sistema seguro de IoT, com uma arquitetura cliente-servidor. 
Este é composto por dois programas que comunicam entre si, o programa cliente 
`IoTDevice` e o programa servidor `IoTServer`.


### Estrutura do projeto


O projeto encontra-se organizado em três packages principais `client`, `server` e `common`.

> ##### Packages:
> - `client` contém código fonte do **IoTDevice**
> - `client.security` contém código relacionado com segurança
> - `server` contém código fonte do **IoTSever**
> - `common` contém código utilizado pelo cliente e servidor
> - `common` contém código utilizado pelo cliente e servidor, relacionado com segurança
> - `server.communication` contém código relacionado com a comunicação com o servidor
> - `server.components` contém código relacionado com os componentes do servidor
> - `server.persistence` contém código relacionado com a persistência do servidor
> - `server.security` contém código relacionado com segurança

**Nota:**

Durante a comunicação entre cliente e servidor serão criados os seguintes diretórios:

- `client` contém os ficheiros que o cliente pede ao servidor
- `server` contém ficheiros com dados do servidor, assim como, dados que o cliente envia ao servidor



### Funcionalidades


O programa `IoTDevice` disponibiliza ao utilizador a seguinte interface de comandos.


> ##### Lista de comandos:
> - `CREATE <dm>` - criar um novo domínio com o nome **dm** no servidor.
> - `ADD <user> <dm> <password-domain>` -  adiciona o **user** ao domínio **dm** utilizando a password do domínio
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

1. Dentro do diretório do projeto, abrir um terminal e executar o script `setup.sh`.
   Verificar se foram criados dois diretórios, um contendo keystores e outro certificados
   
   ```bash
   ./setup.sh
   ```

2. De seguida, continuando dentro do diretório do projeto, execute o script `build.sh`. 
   Verifique se foi criado o diretório `out`

   ```bash
   ./build.sh
   ```

3. Dentro do diretório `out`, executar o seguinte comando para iniciar o servidor

         $ java -jar IoTServer.jar <port> <password-cifra> <keystore> <password-keystore> <2FA-APIKey>

4. Dentro do diretório `out`, executar o seguinte comando para iniciar um cliente

         $ java -jar IoTDevice.jar <IP/hostname>[:Port] <truststore> <keystore> <passwordkeystore> <dev-id> <user-id>


**Notas:** <br>

- Deve alterar e configurar o script `setup.sh`, para gerar os utlizadores pertendidos