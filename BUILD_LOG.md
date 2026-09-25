# Histórico de Desenvolvimento

## 24/09/2026 — Inicialização

### Solicitação/Requisição

Implementar o aplicativo móvel de tarefas conforme a especificação fornecida, mantendo este arquivo atualizado durante o desenvolvimento.

### Resumo da Decisão
- Projeto Android nativo em Kotlin, adequado ao SDK Android disponível e ao escopo móvel solicitado.
- Interface baseada em XML com ViewModel/LiveData para manter o estado separado das telas.
- Room será usado como abstração SQLite; AlarmManager será usado para lembretes locais persistentes.
- Categorias excluídas serão removidas das tarefas por meio de `ON DELETE SET NULL`.

### Ações Executadas
- Criado o arquivo obrigatório `BUILD_LOG.md`.
- Confirmado que a pasta inicial está vazia e que o JDK 17 e o Android SDK estão disponíveis.
- Definida a arquitetura inicial: uma Activity, fragments para as três telas, banco Room e repositório compartilhado.

### Resultado
Estrutura e decisões iniciais definidas; implementação ainda não iniciada.

### Problemas/Erros
- O comando `gradle` não está disponível globalmente; será necessário usar o Gradle Wrapper.
- O primeiro build falhou antes da compilação porque o Android Gradle Plugin não encontrou o SDK sem `local.properties`.

### Tentativas de Correção
- Criado `local.properties` com o caminho local do Android SDK.

### Status Atual
Precisa de testes

## 24/09/2026 — Ajuste de vencimento e validação pelo wrapper

### Solicitação/Requisição
Garantir que o vencimento opcional possa ser removido e validar o projeto pelo Gradle Wrapper.

### Resumo da Decisão
Foi adicionado um comando explícito para remover o vencimento no editor. Ao salvar, o agendador recebe `null` e cancela qualquer alarme existente.

### Ações Executadas
- Adicionado o botão `Remover vencimento` no editor de tarefas.
- Executado `gradlew.bat :app:assembleDebug` usando Gradle 8.7.

### Resultado
- Build final concluído com sucesso.
- O requisito de vencimento opcional agora contempla definir, alterar e remover data/hora.

### Problemas/Erros
- Permanece apenas o aviso não bloqueante de compatibilidade do AGP 8.5.2 com `compileSdk 35`.

### Tentativas de Correção
Nenhuma pendente.

### Status Atual
Precisa de testes

## 24/09/2026 — Núcleo do aplicativo e primeira compilação

### Solicitação/Requisição
Implementar o aplicativo móvel de lista de tarefas em Kotlin com SQLite, múltiplas telas, categorias, filtros, vencimentos e notificações locais.

### Resumo da Decisão
- Room 2.6.1 foi selecionado como camada SQLite, com `Task` e `Category` persistidos e LiveData reativo.
- Uma `MainActivity` troca três fragments: lista, editor e categorias; o ViewModel compartilhado mantém o estado entre telas.
- AlarmManager + BroadcastReceiver foram usados para lembretes locais, com cancelamento e reagendamento pelo ID da tarefa.
- Em Android 13+, a permissão de notificações é solicitada na inicialização; a negação não impede o uso do CRUD.

### Ações Executadas
- Criados Gradle/Kotlin, manifesto, tema e Gradle Wrapper.
- Criados modelos Room, DAOs, banco `todo.db`, ViewModel e operações de CRUD.
- Implementadas as telas de lista, edição/detalhes e categorias.
- Adicionados filtros por status e categoria.
- Adicionados campos de descrição, conclusão, categoria opcional, criação e vencimento opcional.
- Implementados seleção de data/horário, validação de título, exclusão, renomeação e desvinculação por `ON DELETE SET NULL`.
- Adicionado pedido de `POST_NOTIFICATIONS` e agendamento/cancelamento de lembretes.
- Adicionado `.gitignore` para arquivos locais e artefatos de build.

### Resultado
- `:app:assembleDebug` concluído com sucesso.
- O APK de debug é produzido em `app/build/outputs/apk/debug/app-debug.apk`.
- A persistência é feita em SQLite por Room e sobrevive ao fechamento/reabertura do aplicativo.

### Problemas/Erros
- O primeiro build falhou porque faltava `local.properties`; corrigido apontando para o SDK local.
- O segundo build encontrou helpers privados e ausência de `activity-ktx`; ambos corrigidos.
- O AGP 8.5.2 emite apenas um aviso por `compileSdk = 35`; não bloqueia o build.
- Não foi possível executar um teste visual em emulador nesta sessão, pois nenhum dispositivo foi iniciado.

### Tentativas de Correção
- Ajustada a visibilidade dos helpers compartilhados e explicitado o tipo do ViewModel.
- Adicionada `androidx.activity:activity-ktx:1.9.2` para a extensão `viewModels`.
- Reexecutados builds após cada correção; a compilação final passou.

### Status Atual
Concluído

## 24/09/2026 — Revisão final

### Solicitação/Requisição
Revisar os critérios de aceitação e manter o histórico real do desenvolvimento.

### Resumo da Decisão
A implementação atende ao fluxo principal em Android nativo, com estado reativo via LiveData e navegação simples por back stack. A estratégia de notificação é local e opcional: tarefas vencidas, concluídas ou sem vencimento não mantêm alarme ativo.

### Ações Executadas
- Conferidos CRUD de tarefas e categorias, filtros, persistência, seleção de vencimento, permissão e cancelamento de lembretes.
- Gerado Gradle Wrapper versão 8.7.

### Resultado
Aplicativo compilável e funcional para instalação no Android Studio/emulador.

### Problemas/Erros
- Teste de notificação em dispositivo real/emulador e inspeção visual ainda precisam ser executados fora desta sessão.
- O botão de exclusão de categoria não pede confirmação; a exclusão é imediata e desvincula tarefas.

### Tentativas de Correção
Nenhuma pendente para os requisitos centrais.

### Status Atual
Concluído
