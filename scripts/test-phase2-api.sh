#!/bin/bash

# Test Template System Phase 2 API endpoints

API_BASE="http://localhost:8080/api/simple-templates"

echo "=== Template System Phase 2 Test Script ==="
echo ""

# 1. List templates
echo "1. Listing all templates:"
curl -s "${API_BASE}" | jq '.'
echo ""

# 2. Get template parameters (should show new parameter types)
TEMPLATE_ID=$(curl -s "${API_BASE}" | jq -r '.[0].id')
echo "2. Getting parameters for template $TEMPLATE_ID:"
curl -s "${API_BASE}/${TEMPLATE_ID}/parameters" | jq '.'
echo ""

# 3. Test with valid parameters (all new types)
echo "3. Testing with valid parameters (all new types):"
RESPONSE=$(curl -s -X POST "${API_BASE}/${TEMPLATE_ID}/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "Tokyo, Japan",
    "startDate": "2024-12-15",
    "duration": "10",
    "budget": "3000 USD",
    "travelStyle": "Luxury"
  }')
echo "$RESPONSE" | jq '.'
echo ""

# 4. Test with invalid date format
echo "4. Testing with invalid date format:"
curl -s -X POST "${API_BASE}/${TEMPLATE_ID}/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "Paris, France",
    "startDate": "not-a-date",
    "duration": "7"
  }' | jq '.'
echo ""

# 5. Test with invalid currency
echo "5. Testing with invalid currency:"
curl -s -X POST "${API_BASE}/${TEMPLATE_ID}/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "London, UK",
    "startDate": "2024-08-20",
    "duration": "5",
    "budget": "2000 XYZ"
  }' | jq '.'
echo ""

# 6. Test with invalid location (numbers only)
echo "6. Testing with invalid location:"
curl -s -X POST "${API_BASE}/${TEMPLATE_ID}/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "123456",
    "startDate": "2024-09-10",
    "duration": "3"
  }' | jq '.'
echo ""

# 7. Test with missing required parameters
echo "7. Testing with missing required parameters:"
curl -s -X POST "${API_BASE}/${TEMPLATE_ID}/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "Rome, Italy"
  }' | jq '.'
echo ""

# 8. Test with default values (budget and travelStyle should use defaults)
echo "8. Testing with default values:"
curl -s -X POST "${API_BASE}/${TEMPLATE_ID}/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "Barcelona, Spain",
    "startDate": "2024-10-01",
    "duration": "7"
  }' | jq '.'
echo ""

# 9. Test different date formats
echo "9. Testing different date formats:"
for DATE_FORMAT in "2024-11-15" "11/15/2024" "15/11/2024"; do
  echo "   Testing date format: $DATE_FORMAT"
  curl -s -X POST "${API_BASE}/${TEMPLATE_ID}/execute" \
    -H "Content-Type: application/json" \
    -d "{
      \"destination\": \"Sydney, Australia\",
      \"startDate\": \"$DATE_FORMAT\",
      \"duration\": \"14\"
    }" | jq -r '.success'
done
echo ""

echo "=== Test Complete ===" 