package potato;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Function;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import com.google.gson.JsonObject;
import arc.util.Log;

class WebhookClient {
	protected URI uri;
	protected Executor executor;
	protected HttpClient client;

	Function<HttpResponse<String>, HttpResponse<String>> ratelimitHandler = (r) -> {
		if (r.statusCode() == 429) {
			while (true) {
				try {
					Thread.sleep(Float.valueOf(r.headers().firstValue("retry-after").get()).longValue());
				} catch (InterruptedException exception) { continue; }
				break;
			}
			HttpResponse<String> res = client.sendAsync(r.request(), BodyHandlers.ofString()).thenApplyAsync(this.ratelimitHandler).join();
			return res;
		} else {
			return r;
		}
	};

	private CompletableFuture<HttpResponse<String>> execute(JsonObject json) {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(uri)
			.setHeader("Content-Type", "application/json")
			.POST(BodyPublishers.ofString(json.toString()))
			.build();

		CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, BodyHandlers.ofString());
		CompletableFuture<HttpResponse<String>> handlerFuture = future.thenApplyAsync(ratelimitHandler);

		handlerFuture.<Object>thenApply( r -> { Log.debug("Discord response:\n" + r.body()); return null; });
		return handlerFuture;
	}

	public WebhookClient(@NotNull String url, Optional<Executor> executor) {
		Objects.requireNonNull(url, "url");
		this.executor = executor.orElseGet(() -> new ScheduledThreadPoolExecutor(4));
		this.client = HttpClient.newBuilder()
			.executor(this.executor)
			.build();
		this.uri = URI.create(url);
	}

	public String getUrl() {
		return uri.toString();
	}

	public CompletableFuture<HttpResponse<String>> send(@NotNull String content) {
		return send(content, Optional.empty(), Optional.empty());
	}

	public CompletableFuture<HttpResponse<String>> send(@NotNull String content, String username) {
		return send(content, Optional.of(username), Optional.empty());
	}

	public CompletableFuture<HttpResponse<String>> send(@NotNull String content, String username, String avatar) {
		return send(content, Optional.of(username), Optional.of(avatar));
	}

	public CompletableFuture<HttpResponse<String>> send(@NotNull String content, Optional<String> username, Optional<String> avatar) {
		Objects.requireNonNull(content, "content must not be null");

		content = content.trim();
		if (content.isEmpty())
			throw new IllegalArgumentException("Cannot send an empty message");
		if (content.length() > 2000)
			throw new IllegalArgumentException("Content may not exceed 2000 characters");

		JsonObject json = new JsonObject();

		json.addProperty("content", content);

		if (username.isPresent()) {
			json.addProperty("username", username.get());
		}

		if (avatar.isPresent()) {
			json.addProperty("avatar_url", avatar.get());
		}

		return execute(json);
	}
}
