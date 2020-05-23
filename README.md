# gRPC client-server system #

##  Как потестить? ##
1. Клонируем репозиторий - https://github.com/8pade8/GRPSTestProject.git
2. Запускаем сервер - src/main/kotlin/server/Server.kt/main()
3. Запускаем клиент - src/main/kotlin/client/Client.kt/main()
4. В консоли клиента доступны следующие команды:
   - create profile - для создания нового профиля;
   - get profile - для получения информации о профиле
   - add money для увелечения средств на балансе;
   - charge money - для списания средств;
   - exit - для выхода;
5. Далее следуем сообщениям в консоли клиента.
6. В консоль сервера логируются выполняемые запросы.

#### Дополнительная информация ####
В качестве базы данных использована SQLite БД, путь к БД - src/main/kotlin/server/db/profiles.db
Использовать jdk11 не удалось из-за ошибки импортирования аннтоации @javax.annotation.Generated
