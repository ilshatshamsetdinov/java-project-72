package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import static io.ebean.DB.save;


public class UrlController {
    public static Handler listURLs = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;

        PagedList<Url> pagedURLS = new QUrl()
                .setFirstRow(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        List<Url> urls = pagedURLS.getList();

        int lastPage = pagedURLS.getTotalPageCount() + 1;
        int currentPage = pagedURLS.getPageIndex() + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());


        ctx.attribute("urls", urls);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.render("urls/index.html");
    };
    public static Handler createUrl = ctx -> {
        String name = ctx.formParam("url");

        URL url;

        try {
            url = new URL(Objects.requireNonNull(name));
        } catch (MalformedURLException e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
            return;
        }

        String normalizedUrl = url.getProtocol() + "://" + url.getAuthority();

        Url urlFromDb = new QUrl()
                .name.equalTo(normalizedUrl)
                .findOne();

        if (urlFromDb != null) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "info");
            ctx.redirect("/urls");
            return;
        }

        Url urlForSave = new Url(normalizedUrl);
        save(urlForSave);
        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flash-type", "success");

        ctx.redirect("/urls");
    };

    public static Handler showUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        List<UrlCheck> urlChecks = new QUrlCheck()
                .url.equalTo(url)
                .orderBy().id.desc()
                .findList();

        ctx.attribute("urlChecks", urlChecks);
        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };

    public static Handler checkUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse(String.format("Url with id=%d is not found", id));
        }

        try {
            HttpResponse<String> response = Unirest.get(url.getName()).asString();
            Document doc = Jsoup.parse(response.getBody());

            int statusCode = response.getStatus();
            String title = doc.title();

            Element head1 = doc.selectFirst("h1");
            Element descr = doc.selectFirst("meta[name=description]");
            String h1 = (head1 != null) ? head1.text() : "";
            String description = (descr != null) ? descr.attr("content") : "";

            UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, url);
            urlCheck.save();

            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");

        } catch (UnirestException e) {
            ctx.sessionAttribute("flash", "Не удалось проверить страницу");
            ctx.sessionAttribute("flash-type", "danger");
        }

        ctx.redirect("/urls/" + id);
    };

}
