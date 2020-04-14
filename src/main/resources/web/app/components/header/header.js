spa.$('header', {
  style: '.',
  data: {title: 'Universal Cheater!',
         buttonShow: false
  },

  setTitle: function(title, show) {
    if (title == "") {
        title = 'Universal Cheater!'
    }
    this.$data.title = title;
    spa.$render('header', {data: {title: title, buttonShow: show}});
  },

  setBackFunction: function() {

  },

  renderCallback: function() {
    if (this.$data.buttonShow)
        spa.$('header #back_arrow').show();
    else
        spa.$('header #back_arrow').hide();
  }
});