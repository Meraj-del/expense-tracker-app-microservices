import json
from flask import Flask, request, jsonify
from app.service.messageService import MessageService
from kafka import KafkaProducer
import logging
import os

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
message_service = MessageService()

kafka_host = os.getenv('KAFKA_HOST', 'kafka')
kafka_port = os.getenv('KAFKA_PORT', 9092)

producer = None

def get_producer():
    global producer
    if producer is None:
        logger.info(f"Connecting to Kafka at {kafka_host}:{kafka_port}")
        producer = KafkaProducer(
            bootstrap_servers=[f"{kafka_host}:{kafka_port}"],
            value_serializer=lambda v: json.dumps(v).encode('utf-8'),
            request_timeout_ms=30000,
            retries=3,
            retry_backoff_ms=1000
        )
    return producer


@app.route('/v1/ds/message', methods=['POST'])
def handle_message():
    try:
        logger.info("API HIT")

        data = request.get_json(silent=True)

        if not data or "message" not in data:
            return jsonify({"error": "Invalid request"}), 400

        message = data["message"]

        result = message_service.process_message(message)

        if result is None:
            return jsonify({"message": "Not a bank SMS"}), 200

        serialized_result = result.model_dump()

        try:
            get_producer().send('expense_service', serialized_result).get(timeout=10)
            logger.info("Sent to Kafka successfully")
        except Exception as kafka_error:
            logger.error(f"Kafka send failed: {kafka_error}")

        return jsonify(serialized_result), 200

    except Exception as e:
        logger.error(f"ERROR: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/', methods=['GET'])
def handle_get():
    return jsonify({"message": "Hello World"}), 200