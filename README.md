# Search Engine
### Это мой выпускной проект курса JAVA developer от SkillBox

### Описание проекта
SpringBoot приложение. Получает список сайтов из конфигурационного файла,
выполняет их индексацию. Собранную информацию о страницах
сохраняет в базу данных MySQL. На основе полученной информации приложение может
выводить подробную статистику о сайтах, а также выполнять поиск страниц по
заданному запросу.

**Перечень функционала программы**
* Создание таблиц в БД и работ с ней при помощи технологии Hibernate
* Многопоточная индексация сайтов при помощи ForkJoinPool
* Рекурсивный парсинг всех страниц сайта
* Индексация страниц, приведение всех слов страницы в леммы
* Выведение статистики на основе полученных данных
* Возможность поиска страниц с учетом релевантности по заданому запросу
* Возможность поиска страниц для отдельного сайта
* Выведение понятных фрагментов текста из найденных страниц

###  Стэк используемых технологий
* Java 17.0.2
* Spring Boot 2.7.1
* Maven 4.0.0
* ORM Hibernate
* DB MySQL80
* Lombok Library
* JSOUP Library
* Morphology Library


### Инструкцию по запуску проекта