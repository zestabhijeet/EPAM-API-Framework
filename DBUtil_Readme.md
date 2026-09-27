# DBUtil.java Guide

This document explains `src/test/java/utils/DBUtil.java`.

`DBUtil` is a helper class for working with the local H2 test database. It loads DB configuration, creates schema tables, inserts sample data, and prints database records for test verification.

## Configuration

`DBUtil` loads database settings from:

```text
src/test/resources/config.properties
```

Expected properties:

```properties
DB_URL=jdbc:h2:file:./data/testdb
DB_USER=sa
DB_PASSWORD=
DB_DRIVER=org.h2.Driver
```

`ConfigManager.getProperty()` supports key normalization, so `db.url` can resolve from `DB_URL`.

## Static Config Load

Section:

```java
static {
    ConfigManager.load(Paths.get("src/test/resources/config.properties"));
}
```

Purpose:

Loads database properties once when `DBUtil` is first used.

## getConnection()

Method:

```java
public static Connection getConnection()
```

Purpose:

Creates and returns a JDBC connection to the H2 database.

Flow:

1. Loads the DB driver class using `Class.forName(...)`.
2. Reads DB URL, username, and password from config.
3. Creates a connection using `DriverManager.getConnection(...)`.
4. Wraps any connection error in `RuntimeException`.

Used by:

- `createSchema()`
- `insertData()`
- `fetchData()`
- `fetchUserOrders()`
- `UserDBTest.java`

## createSchema()

Method:

```java
public static void createSchema()
```

Purpose:

Drops old tables and creates a fresh database schema.

Drop order:

1. `ORDERS`
2. `ITEMS`
3. `USERS`

This order avoids foreign-key conflicts.

Created tables:

### USERS

Stores user details compatible with ReqRes/user API response fields.

Columns:

- `ID`
- `EMAIL`
- `FIRST_NAME`
- `LAST_NAME`
- `AVATAR`

### ITEMS

Stores item details.

Columns:

- `ITEM_ID`
- `ITEM_DETAILS`
- `ITEM_PRICE`

### ORDERS

Stores user-item order relationships.

Columns:

- `ORDER_ID`
- `ID`
- `ITEM_ID`

Foreign keys:

- `ORDERS.ID` references `USERS.ID`
- `ORDERS.ITEM_ID` references `ITEMS.ITEM_ID`

## insertData()

Method:

```java
public static void insertData()
```

Purpose:

Inserts seed test data after schema creation.

Inserted users:

- `ID 1`: Abhijeet Singh
- `ID 2`: John Doe

Inserted items:

- `ITEM_ID 101`: Laptop
- `ITEM_ID 102`: Mouse

Inserted orders:

- `ORDER_ID 1001`: user `1` ordered item `101`
- `ORDER_ID 1002`: user `2` ordered item `102`

Important:

Call `createSchema()` before `insertData()` to avoid duplicate primary-key errors.

## fetchData()

Method:

```java
public static void fetchData()
```

Purpose:

Reads all rows from the `USERS` table and prints basic user details.

Query:

```sql
SELECT ID, EMAIL, FIRST_NAME, LAST_NAME, AVATAR
FROM USERS
```

Used for:

- Debugging DB setup
- Confirming seed users were inserted
- Printing current user records before API/DB tests

## fetchUserOrders()

Method:

```java
public static void fetchUserOrders()
```

Purpose:

Reads user order details by joining `ORDERS`, `USERS`, and `ITEMS`.

Query joins:

- `ORDERS`
- `USERS`
- `ITEMS`

Output format:

```text
<first name> <last name> ordered <item details>
```

Example:

```text
Abhijeet Singh ordered Laptop
```

## main()

Method:

```java
public static void main(String[] args)
```

Purpose:

Runs the full local DB setup flow manually.

Execution order:

1. `createSchema()`
2. `insertData()`
3. `fetchData()`
4. `fetchUserOrders()`

Use this when you want to verify the database setup without running the full API tests.

## Relationship With UserDBTest

`UserDBTest.java` uses `DBUtil` to:

- Reset the database before tests.
- Insert seed records.
- Open JDBC connections.
- Store API response data in `USERS`.
- Compare DB rows with `response.jsonPath()` values.

Typical setup call in `UserDBTest`:

```java
DBUtil.createSchema();
DBUtil.insertData();
DBUtil.fetchData();
```

## Notes

- The H2 database file is configured as `./data/testdb`.
- `createSchema()` drops existing tables, so it resets previous test data.
- `insertData()` should not be called repeatedly without recreating the schema first.
- Connection handling uses try-with-resources in each method, so connections are closed automatically.
