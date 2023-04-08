# Blockchain

Main:
[![Tests](https://github.com/WorryWarrior/Blockchain/actions/workflows/gradle-tests.yml/badge.svg?branch=main)](https://github.com/WorryWarrior/Blockchain/actions/workflows/gradle-tests.yml)
Develop:
[![Tests](https://github.com/WorryWarrior/Blockchain/actions/workflows/gradle-tests.yml/badge.svg?branch=dev)](https://github.com/WorryWarrior/Blockchain/actions/workflows/gradle-tests.yml)

Аргументы:
- Порт текущего узла
- Порт первого узла-соседа
- Порт второго узла-соседа
- Является ли данный узел основным

# Тестирование

Были реализованы:

- Модульные тесты, тестирующие логику отдельных элементов блокчейна
- Интеграционные тесты, тестирующие взаимодействие элементов блокчейна

Была настроена система CI/CD средствами Github Actions. 
В качестве целевых тестируемых ОС были указаны Linux, Windows, MacOS.

Запуск:
```
java -jar com.example.blockchain-0.0.1.jar 8080 8081 8082 1
java -jar com.example.blockchain-0.0.1.jar 8081 8080 8082 0
java -jar com.example.blockchain-0.0.1.jar 8082 8080 8081 0
```