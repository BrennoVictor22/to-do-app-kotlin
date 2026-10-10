# Questionário do Projeto de Tarefas Android

## 1. Estrutura do projeto

O projeto é um aplicativo Android nativo em Kotlin, com organização simples e centrada em uma Activity principal e vários fragments.

Principais partes:

- UI/screens:
  - `app/src/main/java/com/example/todo/MainActivity.kt`
  - `TaskListFragment` — lista de tarefas
  - `TaskEditorFragment` — criação/edição de tarefas
  - `CategoryFragment` — cadastro e gestão de categorias
- Modelos de dados:
  - `app/src/main/java/com/example/todo/Data.kt`
  - `Task`, `Category`, `TaskWithCategory`
- SQLite / banco de dados:
  - `AppDatabase` e DAOs em `Data.kt`
  - `todo.db` é criado via `Room.databaseBuilder(..., "todo.db")`
- Navegação:
  - `MainActivity.showList()`, `showEditor()`, `showCategories()` em `MainActivity.kt`
- Código de notificação:
  - `ReminderReceiver.kt` — recebe o broadcast do alarme
  - `ReminderScheduler` — agenda e cancela lembretes
  - `AndroidManifest.xml` — registra o receiver e a permissão `POST_NOTIFICATIONS`

A organização geral é bem direta: a `MainActivity` controla a navegação entre telas, o `TodoViewModel` expõe estado reativo para as telas e o banco local é acessado por Room. O app possui uma lista, um editor de tarefa e uma tela de categorias, com persistência em SQLite e lembretes locais.

---

## 2. Arquitetura e estado

O estado da aplicação é gerenciado de forma reativa pelo padrão MVVM (Model-View-ViewModel), embora de forma bem leve e direta.

- `MainActivity` possui um ViewModel compartilhado:
  - `private val model by viewModels<TodoViewModel>()`
- `TodoViewModel` em `TodoViewModel.kt` acessa o banco e expõe:
  - `val tasks = db.taskDao().observeAll()`
  - `val categories = db.categoryDao().observeAll()`
- As telas observam esse estado com `observe(viewLifecycleOwner)`, por exemplo:
  - `model().tasks.observe(viewLifecycleOwner) { list = it; render(it, column) }`
  - `model().categories.observe(viewLifecycleOwner) { ... }`

Como a UI é atualizada após operações:

- criar tarefa:
  - `TaskEditorFragment` coleta título, descrição, categoria, vencimento e concluída.
  - chama `viewModel.saveTask(saved)`
  - a função em `TodoViewModel` executa em `viewModelScope.launch(Dispatchers.IO)`
  - se `task.id == 0L`, faz `db.taskDao().insert(task)`, senão `db.taskDao().update(task)`
  - o `LiveData` do DAO se atualiza e o `TaskListFragment` re-renderiza automaticamente
- editar tarefa:
  - a mesma lógica de `saveTask` é reutilizada, mas com `task.copy(...)`
  - o `TaskEditorFragment` monta um objeto atualizado e salva no banco
  - a lista observa a mudança e atualiza a linha correspondente
- marcar como concluída:
  - `TaskListFragment` usa `model().toggle(task)`
  - `toggle()` chama `saveTask(task.copy(completed = !task.completed))`
  - isso altera o registro em SQLite e a lista reage via `LiveData`

O projeto não usa um gerenciador de estado sofisticado como Redux, Flow/StateFlow do Jetpack ou Compose State; a abordagem é a clássica do Android com `ViewModel + LiveData + Room`, que é um padrão MVVM simples e funcional.

---

## 3. Persistência com SQLite

O projeto usa SQLite via Room, que é uma abstração moderna sobre o banco local do Android.

Onde o banco é criado:

- `Data.kt`, no objeto `AppDatabase`
- `AppDatabase.get(context)` cria a instância com:
  - `Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "todo.db").build()`

Como tarefas e categorias são armazenadas:

- `Task` é uma `@Entity(tableName = "tasks")`
- `Category` é uma `@Entity(tableName = "categories")`
- Há relação entre elas com `ForeignKey(... categoryId ...)` e `onDelete = ForeignKey.SET_NULL`
- Há também `TaskWithCategory`, que faz `LEFT JOIN` com a tabela de categorias para exibir o nome da categoria junto com a tarefa

Operações CRUD:

- `TaskDao`:
  - `observeAll()` — lê todas tarefas com `LiveData`
  - `get(id)` — busca tarefa por ID
  - `insert(task)` — cria tarefa
  - `update(task)` — altera tarefa
  - `delete(task)` — remove tarefa
- `CategoryDao`:
  - `observeAll()` — observa categorias
  - `insert(category)` — cria categoria
  - `update(category)` — edita categoria
  - `delete(category)` — remove categoria

As chamadas de banco ficam encapsuladas no `TodoViewModel`, que dispara as operações em `Dispatchers.IO` para não bloquear a UI.

---

## 4. Acompanhando uma operação: criar tarefa

O fluxo ao pressionar “Salvar” na criação de tarefa é o seguinte:

1. O usuário preenche título, descrição, categoria, vencimento e status em `TaskEditorFragment`.
2. Ao clicar em “Salvar”:
   - o fragment monta `saved = task.copy(...)`
   - o código chama `viewModel.saveTask(saved) { ... }`
3. `TodoViewModel.saveTask()` executa em coroutine:
   - `if (task.id == 0L) db.taskDao().insert(task) else db.taskDao().update(task)`
4. O insert/update acontece no Room/SQLite.
5. A lista em `TaskListFragment` observa `model.tasks` e reage ao `LiveData` emitido pelo DAO.
6. A renderização acontece em `render(source, parent)`, onde as tarefas são filtradas e colocadas como linhas na tela.
7. Se há vencimento, também ocorre agendamento de lembrete:
   - `ReminderScheduler.schedule(requireContext(), saved)`
   - isso cria um `PendingIntent` para `ReminderReceiver` com o ID e o título da tarefa
   - e dispara o `AlarmManager` com `RTC_WAKEUP`
8. Após salvar, a tela fecha com `onBackPressedDispatcher.onBackPressed()`

Funções/componentes principais envolvidos:

- `TaskEditorFragment.onCreateView()`
- `TaskEditorFragment.newInstance(task)`
- `TodoViewModel.saveTask()`
- `TaskDao.insert()` / `update()`
- `TaskListFragment.observe()`
- `TaskListFragment.render()`
- `ReminderScheduler.schedule()`
- `ReminderReceiver.onReceive()`

---

## 5. Navegação

A navegação é simples e feita por troca de fragments dentro da mesma `MainActivity`.

Métodos na `MainActivity`:

- `showList()`: carrega `TaskListFragment`
- `showEditor(task: Task? = null)`: carrega `TaskEditorFragment.newInstance(task)`
- `showCategories()`: carrega `CategoryFragment`

Os fragmentos são alternados com `supportFragmentManager.beginTransaction().replace(...).commit()` e o editor/categorias entram na pilha de volta com `addToBackStack(null)`.

Como a app vai da lista para o editor:

- no botão “+ Nova tarefa”, o listener faz `(activity as MainActivity).showEditor()`
- ao clicar em uma tarefa dentro da listagem, o listener faz `(activity as MainActivity).showEditor(task)`

Quando se está editando, a informação que passa entre telas é um `Task` reconstruído via `Bundle` no `newInstance(task)`, com campos como:

- `id`
- `title`
- `description`
- `completed`
- `due`
- `category`

No `onCreate()` do `TaskEditorFragment`, esses valores são lidos do `Bundle` e transformados em um objeto `Task`:

- `Task(a.getLong("id"), a.getString("title").orEmpty(), ... )`

Ou seja, a informação é passada principalmente por argumentos em `Bundle`, e não por ViewModel compartilhado para a navegação da tela em si.

---

## 6. Implementação de lembretes

Os lembretes de tarefa são implementados com `AlarmManager` + `BroadcastReceiver`.

Como o lembrete é agendado:

- `ReminderScheduler.schedule(context, task)`
- primeiro chama `cancel(context, task.id)` para remover qualquer alarme antigo
- verifica se `dueDateTime` existe, se a tarefa não está concluída e se o vencimento ainda é futuro
- monta um `Intent` para `ReminderReceiver`
- coloca extras `"id"` e `"title"`
- usa `PendingIntent.getBroadcast(...)`
- chama `AlarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pending)`

Como o lembrete é associado à tarefa:

- o `task.id` vira o `requestCode` do `PendingIntent`
- o título da tarefa é enviado em `putExtra("title", task.title)`
- quando o alarme dispara, o `ReminderReceiver` lê esses valores e monta a notificação

O que acontece ao mudar a data de vencimento:

- no editor, quando a data/hora muda, `due` é atualizado e, ao salvar, o código chama `ReminderScheduler.schedule(requireContext(), saved)`
- se a data é removida, o botão “Remover vencimento” define `due = null` e o saved tem `dueDateTime = null`; a próxima vez que o app salvar, o agendamento é feito com `null`, o que impede a criação do alarme

O que acontece ao concluir ou excluir a tarefa:

- ao marcar tarefa como concluída na listagem, o código faz `ReminderScheduler.cancel(requireContext(), task.id)` quando a tarefa fica concluída
- ao excluir tarefa no editor, também há `ReminderScheduler.cancel(requireContext(), task.id)`
- a lógica de cancelamento usa `PendingIntent.getBroadcast` com `FLAG_NO_CREATE` e remove o alarme do `AlarmManager`

No `ReminderReceiver`, o código cria o canal `task_reminders` e chama `NotificationCompat.Builder(...).setContentTitle("Lembrete da tarefa").setContentText(title)`.

---

## 7. Decisões importantes do agente

Pelo menos duas decisões relevantes foram tomadas pelo agente e não eram requisitos explícitos do enunciado:

1. Escolha de Room como camada de persistência
   - em vez de usar SQLite puro com `SQLiteOpenHelper`, o agente usou Room
   - isso trouxe `@Entity`, `@Dao`, `@Database`, `LiveData` e um acesso mais seguro e idiomático ao banco
   - a decisão foi documentada em `BUILD_LOG.md` e é refletida em `Data.kt`

2. Uso de `MainActivity` com fragments em vez de múltiplas Activities
   - o projeto optou por uma `MainActivity` e três fragments (`TaskListFragment`, `TaskEditorFragment`, `CategoryFragment`)
   - isso simplifica a navegação sem exigir uma Activity separada para cada tela
   - foi uma escolha de arquitetura de UI e organização do app

Outras decisões importantes:

- uso de `ViewModel + LiveData` para manter o estado reativo das listas e das categorias
- uso de `AlarmManager` + `BroadcastReceiver` para lembretes locais, em vez de depender de um serviço em segundo plano ou de Firebase/Push
- uso de `ON DELETE SET NULL` para remover categoria sem perder tarefas associadas

---

## 8. Análise do `BUILD_LOG.md`

Um problema/bug encontrado:

- o primeiro build falhou porque faltava `local.properties`, e o Android Gradle Plugin não sabia onde estava o SDK local
- esse problema está registrado em `BUILD_LOG.md` sob “Núcleo do aplicativo e primeira compilação”

Como o agente tentou resolver:

- foi criado o arquivo `local.properties` apontando para o caminho local do Android SDK
- além disso, o build seguinte revelou outros problemas menores: helpers privados e ausência da dependência `androidx.activity:activity-ktx`
- essas correções foram feitas e os builds foram reexecutados

A primeira solução funcionou?

- a criação de `local.properties` resolveu o problema do SDK e foi necessária para a compilação seguir
- os problemas posteriores foram corrigidos em iterações seguintes, não por uma solução única

O que foi feito no final:

- o projeto foi compilado com sucesso via `gradlew.bat :app:assembleDebug`
- o app ficou em `app/build/outputs/apk/debug/app-debug.apk`
- o aviso do AGP sobre `compileSdk = 35` permaneceu, mas não bloqueou o build

O que o build log ajudou a entender que não seria fácil descobrir só olhando o código final:

- mostra a evolução real do desenvolvimento, incluindo erros de build e como eles foram solucionados
- revela que a implementação final foi o resultado de correções iterativas, não apenas do código inicial pronto
- explica a lógica de decisão arquitetural e as escolhas de Room, ViewModel, AlarmManager e notificações
- deixa claro que o projeto passou por adaptações de compatibilidade do Android SDK e de dependências, o que seria invisível se apenas olhêssemos o código final funcionando

---

Resumo final: o projeto é um app Android simples de lista de tarefas com persistência em SQLite usando Room, navegação por fragments em uma Activity, estado reativo via ViewModel/LiveData e lembretes locais com AlarmManager. A estrutura é funcional, bem organizada e está em linha com a arquitetura Android nativa moderna e prática.
