spa.$('systems', {
    dataUrl: 'getSystems',
    dataUrlMethod: 'GET',
    dataProcess: function(apiData) {
        return {
            systems: apiData
        }
    },

    systemClick: function(sys) {
        app.main.setSystem(sys);
    }
});

