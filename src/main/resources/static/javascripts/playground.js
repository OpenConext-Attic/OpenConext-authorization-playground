$(function () {

  // show the correct step
  $(function () {
    $('#' + $('input#step').val()).collapse('show');
  });

  // changing the grant type means toggling some fields:
  $("input[name='grantType']").click(function () {
    $('[data-show-client-credentials]').toggle(this.id !== 'clientCredentials');
    $('[data-show-implicit]').toggle(this.id !== 'implicitGrant');
    $('[data-show-auth-code]').toggle(this.id === 'authCode');
  });


  // we are in step 3 of implicit grant
  if ($('#parseAnchorForAccessToken').val() == 'true') {
    var hash = window.location.hash.replace("#", "");
    $('#parseAnchorForAccessToken').val('');

    $('#responseInfo').html(hash);
    var accessToken;
    $.each(hash.split("&"), function (i, value) {
      var param = value.split("=");
      if (param[0] == 'access_token') {
        accessToken = param[1];
        $('#accessToken').val(accessToken);
      }
    });
    if ($('#openIdConnect').val() == 'true') {
      var jqxhr = $.get("decodeJwtToken?jwtToken=" + accessToken).done(function () {
        $('#accessTokenJsonPrint').html(jqxhr.responseText);
        $('#accessTokenJsonPrintInput').val(jqxhr.responseText)

      });
    }

  }

  // we are in step 3 of id_token
  if ($('#parseAnchorForIdToken').val() == 'true') {
    var hash = window.location.hash.replace("#", "");
    $('#parseAnchorForIdToken').val('');

    $('#responseInfo').html(hash);
    var oidcIdToken;
    $.each(hash.split("&"), function (i, value) {
      var param = value.split("=");
      if (param[0] == 'id_token') {
        oidcIdToken = param[1];
        $('#oidcIdToken').val(oidcIdToken);
      }
    });
    var jqxhr = $.get("decodeJwtToken?jwtToken=" + oidcIdToken).done(function () {
      $('#idTokenJsonPrint').html(jqxhr.responseText);
      $('#idTokenJsonPrintInput').val(jqxhr.responseText)
    });
  }

});
