# Search Engine
### Это мой выпускной проект курса JAVA developer от SkillBox

### Описание проекта
SpringBoot приложение. Поисковой движок, позваляет быстро 
находить страницы сайтов по заданному запросу.

**Принципы работы поискового движка**

1. В конфигурационном файле перед запуском приложения задаются
   адреса сайтов, по которым движок осуществляет поиск.
2. Поисковый движок самостоятельно находит все страницы
   заданных сайтов и индексирует их (сохраняет все небходиммые данные о странице)
   так, чтобы потом находить наиболее релевантные страницы по любому
   поисковому запросу.
3. Пользователь присылает запрос через API движка. Запрос — это набор
   слов, по которым нужно найти страницы сайта.
4. В индексе ищутся страницы, на которых встречаются все эти слова.
5. Результаты поиска ранжируются, сортируются и отдаются пользователю.


**Перечень функционала программы**
* Создание таблиц в БД и работ с ней при помощи технологии Hibernate
* Многопоточная индексация сайтов при помощи ForkJoinPool
* Рекурсивный парсинг всех страниц заданных сайтов
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

### Спецификация API
* **GET /api/startIndexing**
   >Метод запускает полную индексацию всех сайтов или полную
  переиндексацию, если они уже проиндексированы.
  Если в настоящий момент индексация или переиндексация уже
  запущена, метод возвращает соответствующее сообщение об ошибке.
   > 
   >**Параметры:** Метод без параметров.
  
* **GET /api/stopIndexing**
   >Метод останавливает текущий процесс индексации (переиндексации).
  Если в настоящий момент индексация или переиндексация не происходит,
  метод возвращает соответствующее сообщение об ошибке.
   >
   >**Параметры:** Метод без параметров.
* **POST /api/indexPage**
   >Метод добавляет в индекс или обновляет отдельную страницу, адрес
  которой передан в параметре.
  Если адрес страницы передан неверно, метод должен вернуть
  соответствующую ошибку.
   >
   >**Параметры:**
   > * url — адрес страницы, которую нужно переиндексировать.

* **GET /api/statistics**
   >Метод возвращает статистику и другую служебную информацию о
    состоянии поисковых индексов и самого движка.
   >
   >**Параметры:** Метод без параметров.

* **GET /api/search**
   >  Метод осуществляет поиск страниц по переданному поисковому запросу.
    В ответе выводится общее количество результатов, и массив с результатами поиска.
    Если поисковый запрос не задан или ещё нет готового индекса (сайт, по
    которому ищем, или все сайты сразу не проиндексированы), мотод возвращает
    соответствующую ошибку. 
  > 
  >**Параметры:** 
   > * query — поисковый запрос;
   > * site — сайт, по которому осуществлять поиск, если не задан, поиск
       происходит по всем проиндексированным сайтам.
   > * offset — сдвиг от 0 для постраничного вывода (параметр
       необязательный; если не установлен, то значение по умолчанию равно
       нулю);
   > * limit — количество результатов, которое необходимо вывести (параметр
        необязательный; если не установлен, то значение по умолчанию равно
        20).

       
### Инструкцию по запуску проекта

### Работа с веб-интерфейсом
Веб-интерфейс представляет собой одну веб-страницу с тремя вкладками:
1. **Dashboard** - На ней отображается общая статистика по всем сайтам, количесвто
заданных сайтов, количество проиндексированных страниц, количесвто найденных лемм
в проиндексированных страницах. А также детальная статистика и статус по каждому из сайтов, 
время последней индексации (периндексации), найденные леммы, страницы для данного сайта,
последняя ошибка если таковая имеется.
    > Для получения актуальной информации не забывайте обновлять страницу. 

2. **Management** - На этой вкладке находятся инструменты управления
   поисковым движком. 

   Кнопка **"START INDEXING" или "STOP INDEXING"** - запуск и остановка полной индексации
   (переиндексации).

   Кнопка **ADD/UPDATE** - добавить (обновить) отдельную
   страницу по ссылке.


3. **Search** - Эта страница предназначена для тестирования поискового
   движка. На ней находится поле поиска, выпадающий список с выбором
   сайта для поиска, а при нажатии на кнопку **"SEARCH"** выводятся
   результаты поиска по запросу написанному в поле "Query"
