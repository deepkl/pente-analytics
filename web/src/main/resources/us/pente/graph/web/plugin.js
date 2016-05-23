require([
    'flight/lib/component',
    'configuration/plugins/registry',
    'util/vertex/formatters',
    'util/messages',
    'react',
    'react-dom'
], function (
    defineComponent,
    registry,
    F,
    i18n,
    React,
    ReactDOM) {
    'use strict';

    // Details pane toolbar extension to open user profile page

    registry.registerExtension('org.visallo.detail.toolbar', {
        title:  i18n('us.pente.graph.web.detail.toolbar.player.profile'),
        event: 'openPlayerProfile',
        canHandle: function(objects) {
            return objects.vertices.length === 1 && objects.edges.length === 0
                && objects.vertices[0].conceptType === 'http://pente.us/pente#player';
        }
    });

    registry.registerExtension('org.visallo.detail.toolbar', {
        title:  i18n('us.pente.graph.web.detail.toolbar.game'),
        event: 'openGame',
        canHandle: function(objects) {
            return objects.vertices.length === 1 && objects.edges.length === 0
                && objects.vertices[0].conceptType === 'http://pente.us/pente#game';
        }
    });

    $(document).on('openPlayerProfile', function(e, data) {
        var playerName = F.vertex.prop(data.vertices[0], 'http://pente.us/pente#playerName');
        var url = 'https://pente.org/gameServer/profile?viewName=' + playerName;
        window.open(url, '_blank');
    });

    $(document).on('openGame', function(e, data) {
        var gameId = data.vertices[0].id.substring(5); // id has prefix 'GAME_'
        var url = 'https://pente.org/gameServer/viewLiveGame?mobile&g=' + gameId;
        window.open(url, '_blank');
    });


    // Graph view extension to test React

    registry.registerExtension('org.visallo.graph.view', {
        componentPath: 'myplugins/hello_world',
        className: 'hello-world-example'
    });

    define('myplugins/hello_world', ['jsx!us/pente/graph/web/HelloWorld'], function(HelloWorld) {
        return defineComponent(function() {
            this.after('initialize', function () {
                ReactDOM.render(
                    React.createElement(HelloWorld, {message: 'Hi!'}),
                    this.$node[0]);
            });
        });
    });
});
