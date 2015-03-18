$(function () {

  // show the correct step
  $(function () {
    $('#' + $('input#step').val()).collapse('show');
  });

  // changing the grant type means toggling some fields:
  $("input[name='grantType']").click(function () {
    $('[data-show-client-credentials]').toggle(this.id !== 'clientCredentials');
    $('[data-show-implicit]').toggle(this.id !== 'implicitGrant');
  });


  // we are in step 3 of implicit grant
  if ($('#parseAnchorForAccessToken').val() == 'true') {
    var hash = window.location.hash.replace("#", "");
    $('#parseAnchorForAccessToken').val('');

    $('#responseInfo').html(hash);
    $.each(hash.split("&"), function (i, value) {
      var param = value.split("=");
      if (param[0] == 'access_token') {
        $('#accessToken').val(param[1]);
      }
    });

  }

});
