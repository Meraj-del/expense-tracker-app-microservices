FROM python:3.11-slim
WORKDIR /app

COPY ds_service-1.0.tar.gz .
COPY .env .env

RUN pip install --no-cache-dir flask \
    && pip install --no-cache-dir ds_service-1.0.tar.gz

ENV FLASK_APP=app

EXPOSE 8000

CMD ["python", "-m", "flask", "run", "--host=0.0.0.0", "--port=8000"]
