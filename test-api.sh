#!/bin/bash

# Тест API на VPS
API_URL="http://45.8.228.89:8080"

echo "🧪 Тестируем API на ${API_URL}"

# 1. Проверка работоспособности
echo "1. Проверка API..."
curl -f ${API_URL}/ && echo " ✅ API работает" || echo " ❌ API не отвечает"

# 2. Загрузка изображения (замени test.jpg на реальный файл)
if [[ -f "test.jpg" ]]; then
    echo "2. Загружаем изображение..."
    RESPONSE=$(curl -s -X POST -F 'image=@test.jpg' ${API_URL}/upload)
    echo "Ответ: $RESPONSE"

    # Извлекаем imageId из ответа
    IMAGE_ID=$(echo $RESPONSE | grep -o '"imageId":"[^"]*' | cut -d'"' -f4)

    if [[ -n "$IMAGE_ID" ]]; then
        echo "✅ Изображение загружено: $IMAGE_ID"

        # 3. Получаем ссылку на изображение
        echo "3. Получаем ссылку на изображение..."
        curl -s ${API_URL}/link/${IMAGE_ID} | jq .

        # 4. Получаем HTML с изображением
        echo "4. Получаем HTML..."
        curl -s ${API_URL}/html-link/${IMAGE_ID} | jq -r '.html' | base64 -d > result.html
        echo "HTML сохранен в result.html"

        echo "🌐 Превью: ${API_URL}/preview/${IMAGE_ID}"
    else
        echo "❌ Ошибка загрузки изображения"
    fi
else
    echo "⚠️  Для теста создайте файл test.jpg"
fi
