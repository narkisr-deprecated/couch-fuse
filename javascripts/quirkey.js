var Quirkey = {
	initialize: function() {
		this.quirkey_nav.initialize();
	},
	quirkey_nav: {
		opened: false,
		initialize: function() {
			var port = this;
			$('.quirkey_nav_open').click(function() {
				if (port.opened) {
					port.close();
				} else {
					port.open();
				}
			});
			$('.quirkey_nav_close').click(function() {
				port.close();
			});
		},
		open: function() {
			this.initial_top = $('#logo').css('top');
			$('#logo').animate({'top': '0px'}, 400);
			$('#quirkey_nav').slideDown(400, function() {
				$('.quirkey_nav_close').slideDown();
			});
			this.opened = true;
		},
		close: function() {
			$('.quirkey_nav_close').hide();
			$('#logo').animate({'top': this.initial_top}, 400);
			$('#quirkey_nav').slideUp(400);
			this.opened = false;
		}
	}
};


$(function() {
	Quirkey.initialize();
})