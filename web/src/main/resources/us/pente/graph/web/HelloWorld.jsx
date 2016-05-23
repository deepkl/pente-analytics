define([], function() {
    var React = require('react');

    return React.createClass({
        render: function() {
            const message = this.props.message;
            return (
                <h1>{message}</h1>
            );
        }
    });
});
