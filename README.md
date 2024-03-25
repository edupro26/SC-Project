# SC_Projeto - 2023/2024

Projeto da disciplina Seguranca e Confiabilidade

O projeto consiste em duas aplicacoes, cliente (IoTDevice) e servidor (IoTServer) que comunicam entre si. Varios clientes podem-se conectar ao servido e realizar neste
varios comandos especificados no enunciado do projeto.

##Autores
Trabalho realizado por:
  Grupo 6

- Eduardo Proenca - 57551
- Manuel Barral - 52026
- Tiago Oliveira - 54979

##Compilacao

Para compilar este projeto basta executar o ficheiro build.sh.
Alternativamente, podemos compilar o projeto usando o maven executando o comando maven.sh.

##Execucao

Depois de se ter feito build do projeto, deve-se inicializar o servidor ao executar o ficheiro runServer.sh ou entao abrir uma consola na root do projeto e executar o comando 'java -jar IoTServer-grupo6.jar <port>'. Podemos omitir o argumento porto, neste caso o servidor e inicializado com um porto default '12345'.

Em seguida, executar runClient.sh para inicializar cliente. Neste caso, o user em questao e aquele que esta no script, para usar o seu proprio user basta abrir uma consola na root do projeto e executar o comando 'java -jar IoTDevice-grupo6.jar <serverAdress devId user>'.

##Notas adicionais
Se o servidor continuar a responder NOK-TESTED depois de um utilizador tentar efetuar o login, verifique se o tamanho do .jar do cliente corresponde a um dos tamanhos no ficheiro device_info na pasta src/main/resources.
