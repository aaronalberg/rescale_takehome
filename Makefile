test:
	docker buildx build --platform linux/amd64 --tag testsuite --target testsuite . && docker run --platform linux/amd64 testsuite
run:
	docker-compose build && docker-compose up