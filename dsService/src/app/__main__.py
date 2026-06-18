from . import app

if __name__ == "__main__":
    print("Running from __main__.py")
    app.run(host="192.168.1.50", port=8000, debug=False)