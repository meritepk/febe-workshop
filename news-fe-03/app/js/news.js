(function () {
    'use strict';
    angular.module("news-fe", ['ngResource', 'ng']);

    function NewsService($resource) {
        return $resource('../news/api/v1/news/:id');
    }

    angular.module('news-fe').factory('NewsService', ['$resource', NewsService]);

    function NewsController(NewsService) {
        var self = this;

        self.service = NewsService;
        self.news = [];
        self.title = '';
        self.display = false;

        self.init = function () {
            self.search();
        }

        self.search = function () {
            self.display = false;
            var parameters = {};
            if (self.title) {
                parameters.title = self.title;
            }
            self.service.get(parameters).$promise.then(function (response) {
                self.display = true;
                self.news = response.content;
            });
        }

        self.init();
    }

    angular.module("news-fe").controller('NewsController', ['NewsService', NewsController]);

}());