#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// UUIDs must match your Android app’s SERVICE and CHARACTERISTIC UUIDs
#define SERVICE_UUID        "6e400001-b5a3-f393-e0a9-e50e24dcca7e"
#define CHARACTERISTIC_UUID "6e400002-b5a3-f393-e0a9-e50e24dcca5e"

BLECharacteristic *pCharacteristic;
bool deviceConnected = false;

// --- Example GPIO pins for controlling relays or LEDs ---
#define BEDROOM1_PIN  5   // GPIO5
#define BEDROOM2_PIN  18  // GPIO18
#define BEDROOM3_PIN  19  // GPIO19
#define BEDROOM4_PIN  21  // GPIO21
#define HALL_PIN      22  // GPIO22
#define KITCHEN_PIN   23  // GPIO23

#define ALLON_PIN     25  // GPIO25
#define ALLOFF_PIN    26  // GPIO26

// --- Callback for received data ---
class toggleCallback : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) override {
    String value = pCharacteristic->getValue();  // Get the written string
    value.trim(); // remove leading/trailing whitespace

    if (value.length() > 0) {
      Serial.print("Received command: ");
      Serial.println(value);

      // ====== BEDROOMS ======
      if (value == "b1_on") {
        Serial.println("Bedroom 1 ON");
        digitalWrite(BEDROOM1_PIN, HIGH);
      } else if (value == "b1_off") {
        Serial.println("Bedroom 1 OFF");
        digitalWrite(BEDROOM1_PIN, LOW);
      }

      else if (value == "b2_on") {
        Serial.println("Bedroom 2 ON");
        digitalWrite(BEDROOM2_PIN, HIGH);
      } else if (value == "b2_off") {
        Serial.println("Bedroom 2 OFF");
        digitalWrite(BEDROOM2_PIN, LOW);
      }

      else if (value == "b3_on") {
        Serial.println("Bedroom 3 ON");
        digitalWrite(BEDROOM3_PIN, HIGH);
      } else if (value == "b3_off") {
        Serial.println("Bedroom 3 OFF");
        digitalWrite(BEDROOM3_PIN, LOW);
      }

      else if (value == "b4_on") {
        Serial.println("Bedroom 4 ON");
        digitalWrite(BEDROOM4_PIN, HIGH);
      } else if (value == "b4_off") {
        Serial.println("Bedroom 4 OFF");
        digitalWrite(BEDROOM4_PIN, LOW);
      }

      // ====== HALL ======
      else if (value == "h1_on") {
        Serial.println("Hall ON");
        digitalWrite(HALL_PIN, HIGH);
      } else if (value == "h1_off") {
        Serial.println("Hall OFF");
        digitalWrite(HALL_PIN, LOW);
      }

      // ====== KITCHEN ======
      else if (value == "k1_on") {
        Serial.println("Kitchen ON");
        digitalWrite(KITCHEN_PIN, HIGH);
      } else if (value == "k1_off") {
        Serial.println("Kitchen OFF");
        digitalWrite(KITCHEN_PIN, LOW);
      }

      // ====== ALL ON / ALL OFF ======
      else if (value == "all1") {
        Serial.println("Turning ALL ON");
        digitalWrite(BEDROOM1_PIN, HIGH);
        digitalWrite(BEDROOM2_PIN, HIGH);
        digitalWrite(BEDROOM3_PIN, HIGH);
        digitalWrite(BEDROOM4_PIN, HIGH);
        digitalWrite(HALL_PIN, HIGH);
        digitalWrite(KITCHEN_PIN, HIGH);
      } 
      else if (value == "all2") {
        Serial.println("Turning ALL OFF");
        digitalWrite(BEDROOM1_PIN, LOW);
        digitalWrite(BEDROOM2_PIN, LOW);
        digitalWrite(BEDROOM3_PIN, LOW);
        digitalWrite(BEDROOM4_PIN, LOW);
        digitalWrite(HALL_PIN, LOW);
        digitalWrite(KITCHEN_PIN, LOW);
      } 

      else {
        Serial.print("Unknown command: ");
        Serial.println(value);
      }
    }
  }
};


// --- BLE setup ---
void setupBLE() {
  BLEDevice::init("ESP32_Meridian");
  BLEServer *pServer = BLEDevice::createServer();
  BLEService *pService = pServer->createService(SERVICE_UUID);

  pCharacteristic = pService->createCharacteristic(
      CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_WRITE
  );

  pCharacteristic->setCallbacks(new toggleCallback());
  pCharacteristic->addDescriptor(new BLE2902());

  pService->start();
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->start();

  Serial.println("BLE Ready: Waiting for client...");
}

void setup() {
  Serial.begin(115200);

  pinMode(BEDROOM1_PIN, OUTPUT);
  pinMode(BEDROOM2_PIN, OUTPUT);
  pinMode(BEDROOM3_PIN, OUTPUT);
  pinMode(BEDROOM4_PIN, OUTPUT);
  pinMode(HALL_PIN,     OUTPUT);
  pinMode(KITCHEN_PIN,  OUTPUT);
  pinMode(ALLON_PIN,    OUTPUT);
  pinMode(ALLOFF_PIN,   OUTPUT);

  // Initialize all outputs LOW
  digitalWrite(BEDROOM1_PIN,  LOW);
  digitalWrite(BEDROOM2_PIN,  LOW);
  digitalWrite(BEDROOM3_PIN,  LOW);
  digitalWrite(BEDROOM4_PIN,  LOW);
  digitalWrite(HALL_PIN,      LOW);
  digitalWrite(KITCHEN_PIN,   LOW);
  digitalWrite(ALLON_PIN,     LOW);
  digitalWrite(ALLOFF_PIN,    LOW);
  setupBLE();
  Serial.println("ESP32 Meridian Controller Ready");
}

void loop() {
  // Nothing needed — BLE callbacks handle everything
}

