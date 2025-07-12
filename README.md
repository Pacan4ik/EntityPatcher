# Patcher

Патчинг (копирование/преобразование) полей и значений между двумя объектами.

## Возможности

- Динамическое сопоставление полей по имени  
- Настраиваемые стратегии маппинга (методы, поля)  
- Глобальные и полевые трансформеры  
- Игнорирование null-значений  
- Логирование изменений  
- Валидация результата патчинга
- Поддержка аннотаций

```java
import ru.andryxx.patcher.engine.Patcher;
import ru.andryxx.patcher.mapping.MappingStrategy;
import ru.andryxx.patcher.validation.PatchValidator;

// 1. Создаём патчер между DTO и Entity
var patcher = Patcher.forType(MyDto.class, MyEntity.class)
    .withMappingStrategy(MappingStrategy.USE_METHODS_AND_FIELDS)
    .ignoreNull()                           // глобально игнорировать null
    .withFieldMapping("foo", "bar")         // настраиваемое сопоставление полей
    .withTransformer(Integer.class, String.class, Object::toString) // глобальный трансформер
    .withValidator(new MyCustomValidator()); // валидатор результата

// 2. Выполняем «патч» данных
MyDto dto = new MyDto(/* … */);
MyEntity entity = new MyEntity();
patcher.patch(dto, entity);

// Entity теперь содержит скопированные/преобразованные значения из DTO
```