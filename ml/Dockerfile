FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY api api
COPY models models
COPY train_model.py train_model.py
EXPOSE 5000
ENTRYPOINT ["sh", "-c", "python api/flask_api.py"]
